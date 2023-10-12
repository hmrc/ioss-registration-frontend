/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.euDetails.EuVatNumberFormProvider
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, CountryWithValidationDetails, Index, UserAnswers}
import models.core.{Match, MatchType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuVatNumberView

import scala.concurrent.Future

class EuVatNumberControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == country.code).value

  val formProvider = new EuVatNumberFormProvider()
  val form: Form[String] = formProvider(country)

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(SellsGoodsToEuConsumerMethodPage(countryIndex), EuConsumerSalesMethod.FixedEstablishment).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.VatNumber).success.value

  private lazy val euVatNumberRoute: String = routes.EuVatNumberController.onPageLoad(waypoints, countryIndex).url
  private lazy val euVatNumberSubmitRoute: String = routes.EuVatNumberController.onSubmit(waypoints, countryIndex).url

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    "33333333",
    None,
    "DE",
    None,
    None,
    None,
    None,
    None
  )

  "EuVatNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuVatNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, countryIndex, countryWithValidation)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answers.set(EuVatNumberPage(countryIndex), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val view = application.injector.instanceOf[EuVatNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), waypoints, countryIndex, countryWithValidation)(request, messages(application)).toString
      }
    }

    "must save and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {
        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn
          Future.successful(None)

        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=FixedEstablishmentActiveNETP" in {

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(genericMatch))

        when(mockCoreRegistrationValidationService.isActiveTrader(genericMatch)) thenReturn true

        val request =
          FakeRequest(POST, euVatNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(waypoints, countryIndex).url
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=TraderIdActiveNETP" in {

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isActiveTrader(expectedResponse)) thenReturn true

        val request =
          FakeRequest(POST, euVatNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(waypoints, countryIndex).url
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=OtherMSNETPActiveNETP" in {

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isActiveTrader(expectedResponse)) thenReturn true

        val request =
          FakeRequest(POST, euVatNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(waypoints, countryIndex).url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match FixedEstablishmentQuarantinedNETP " in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isQuarantinedTrader(expectedResponse)) thenReturn true

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match TraderIdQuarantinedNETP " in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isQuarantinedTrader(expectedResponse)) thenReturn true

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match OtherMSNETPQuarantinedNETP " in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isQuarantinedTrader(expectedResponse)) thenReturn true

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to the next page when there is no active trader" in {

      val application = applicationBuilder(userAnswers = Some(answers))

        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isActiveTrader(expectedResponse)) thenReturn false

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must redirect to the next page when there is no excluded trader" in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockCoreRegistrationValidationService.isQuarantinedTrader(expectedResponse)) thenReturn false

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must redirect to the next page when no active match found" in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(None)

        val request = FakeRequest(POST, euVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuVatNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, countryIndex, countryWithValidation)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
