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
import models.euDetails.RegistrationType
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.CannotRegisterFixedEstablishmentOperationOnlyView

class CannotRegisterFixedEstablishmentOperationOnlyControllerSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)
  private val country1: Country = arbitraryCountry.arbitrary.sample.value
  private val country2: Country = arbitraryCountry.arbitrary.sample.value
  private val euTaxReference: String = arbitraryEuTaxReference.sample.value
  private val tradingName = stringsWithMaxLength(40).sample.value

  private val answers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex1), country1).success.value

  private lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterFixedEstablishmentOperationOnlyController.onPageLoad(waypoints, countryIndex1).url

  "CannotRegisterFixedEstablishmentOperationOnly Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRegisterFixedEstablishmentOperationOnlyView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, countryIndex1)(request, messages(application)).toString
      }
    }

    "must delete the country and redirect to the correct page when there is only one country present" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answers
          .set(HasFixedEstablishmentPage(countryIndex1), false).success.value
          .remove(EuDetailsQuery(countryIndex1)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          CannotRegisterFixedEstablishmentOperationOnlyPage(countryIndex1).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must delete the country and redirect to the correct page when there are multiple countries present" in {

      lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterFixedEstablishmentOperationOnlyController.onPageLoad(waypoints, countryIndex2).url

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val answersWithMultipleCountries: UserAnswers = answers
        .set(HasFixedEstablishmentPage(countryIndex1), true).success.value
        .set(RegistrationTypePage(countryIndex1), RegistrationType.TaxId).success.value
        .set(EuTaxReferencePage(countryIndex1), euTaxReference).success.value
        .set(FixedEstablishmentTradingNamePage(countryIndex1), tradingName).success.value
        .set(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value).success.value
        .set(AddEuDetailsPage(Some(countryIndex1)), true).success.value
        .set(EuCountryPage(countryIndex2), country2).success.value
        .set(HasFixedEstablishmentPage(countryIndex2), false).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithMultipleCountries))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answersWithMultipleCountries
          .remove(EuDetailsQuery(countryIndex2)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          CannotRegisterFixedEstablishmentOperationOnlyPage(countryIndex2).navigate(waypoints, answersWithMultipleCountries, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
