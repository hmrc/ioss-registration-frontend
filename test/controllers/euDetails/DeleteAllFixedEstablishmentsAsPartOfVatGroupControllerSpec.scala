/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.euDetails

import base.SpecBase
import models.amend.RegistrationWrapper
import models.euDetails.{EuDetails, RegistrationType}
import models.requests.AuthenticatedDataRequest
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import pages.euDetails.{DeleteAllFixedEstablishmentsAsPartOfVatGroupPage, TaxRegisteredInEuPage}
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps
import viewmodels.DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel
import views.html.euDetails.DeleteAllFixedEstablishmentsAsPartOfVatGroupView

class DeleteAllFixedEstablishmentsAsPartOfVatGroupControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  private val ossEnrolmentKey: String = "HMRC-OSS-ORG"
  private val iossEnrolmentKey: String = "HMRC-IOSS-ORG"
  private val ossEnrolment: Enrolment = Enrolment(ossEnrolmentKey, Seq(EnrolmentIdentifier("VRN", vrn.vrn)), "Activated")
  private val iossEnrolment: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)), "Activated")
  private val enrolments: Enrolments = Enrolments(Set(ossEnrolment, iossEnrolment))

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val country: Country = Country.fromCountryCodeUnsafe(euVatNumber.substring(0, 2))
  private val euDetails: EuDetails = EuDetails(
    euCountry = country,
    hasFixedEstablishment = Some(true),
    registrationType = Some(RegistrationType.VatNumber),
    euVatNumber = Some(euVatNumber),
    euTaxReference = None,
    fixedEstablishmentTradingName = Some("Test Trading Name"),
    fixedEstablishmentAddress = Some(arbitraryInternationalAddress.arbitrary.sample.value)
  )

  private val euDetailsList: List[EuDetails] = List(euDetails)

  private val answers: UserAnswers = completeUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(AllEuDetailsQuery, euDetailsList).success.value

  private lazy val deleteAllFixedEstablishmentsAsPartOfVatGroupRoute:String = routes.DeleteAllFixedEstablishmentsAsPartOfVatGroupController.onPageLoad().url

  private val registrationPartOfVatGroup: RegistrationWrapper = registrationWrapper.copy(
    vatInfo = registrationWrapper.vatInfo.copy(partOfVatGroup = true)
  )

  private lazy val aRequest = AuthenticatedDataRequest(FakeRequest(GET, deleteAllFixedEstablishmentsAsPartOfVatGroupRoute), testCredentials, vrn, enrolments, Some(iossNumber), answers, Some(registrationPartOfVatGroup), 1, None)
  private lazy val dataRequest: AuthenticatedDataRequest[AnyContentAsEmpty.type] = AuthenticatedDataRequest(aRequest, testCredentials, vrn, enrolments, Some(iossNumber), answers, Some(registrationPartOfVatGroup), 1, None)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationService)
  }

  "DeleteAllFixedEstablishmentsAsPartOfVatGroup Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationService.toUserAnswers(any(), any(), any())) thenReturn answers.toFuture

      val application = applicationBuilder(
        userAnswers = Some(answers),
        registrationWrapper = Some(registrationPartOfVatGroup)
      )
        .overrides(
          bind[RegistrationService].toInstance(mockRegistrationService)
        )
        .build()

      running(application) {
        val request = dataRequest

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllFixedEstablishmentsAsPartOfVatGroupView]

        val viewModel: DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel = DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel(euDetailsList)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(viewModel)(request, messages(application)).toString
        verify(mockRegistrationService, times(1)).toUserAnswers(eqTo(request.userId), eqTo(request.registrationWrapper.value), eqTo(true))
      }
    }

    "must throw an Illegal State Exception when registration is missing from the request for a GET" in {

      val errorMessage: String = "Registration not available. Must have a registration."

      val application = applicationBuilder(
        userAnswers = Some(answers)
      )
        .build()

      running(application) {
        val request = dataRequest.copy(
          request = aRequest.copy(registrationWrapper = None),
          registrationWrapper = None
        )

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp `mustBe` a[IllegalStateException]
          exp.getMessage `mustBe` errorMessage
        }
        verifyNoInteractions(mockRegistrationService)
      }
    }

    "must remove the answers and redirect to the next page when valid data is submitted" in {

      val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllFixedEstablishmentsAsPartOfVatGroupRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answers
          .remove(AllEuDetailsQuery).success.value
          .set(TaxRegisteredInEuPage, false).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllFixedEstablishmentsAsPartOfVatGroupPage.navigate(EmptyWaypoints, answers, expectedAnswers).url
        verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
        verifyNoInteractions(mockRegistrationService)
      }
    }
  }
}
