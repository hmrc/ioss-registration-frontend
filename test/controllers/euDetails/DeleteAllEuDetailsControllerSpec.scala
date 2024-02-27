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
import forms.euDetails.DeleteAllEuDetailsFormProvider
import models.euDetails.RegistrationType
import models.{Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.AllEuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.DeleteAllEuDetailsView

class DeleteAllEuDetailsControllerSpec extends SpecBase with MockitoSugar {
  
  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)
  
  val formProvider = new DeleteAllEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider()
  
  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex1), arbitraryCountry.arbitrary.sample.value).success.value
    .set(HasFixedEstablishmentPage(countryIndex1), true).success.value
    .set(RegistrationTypePage(countryIndex1), RegistrationType.TaxId).success.value
    .set(EuTaxReferencePage(countryIndex1), arbitraryEuTaxReference.sample.value).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex1), "Trading name one").success.value
    .set(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value).success.value
    .set(EuCountryPage(countryIndex2), arbitraryCountry.arbitrary.sample.value).success.value
    .set(HasFixedEstablishmentPage(countryIndex2), true).success.value
    .set(RegistrationTypePage(countryIndex2), RegistrationType.TaxId).success.value
    .set(EuTaxReferencePage(countryIndex2), arbitraryEuTaxReference.sample.value).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex2), "Trading name two").success.value
    .set(FixedEstablishmentAddressPage(countryIndex2), arbitraryInternationalAddress.arbitrary.sample.value).success.value

  lazy val deleteAllEuDetailsRoute: String = routes.DeleteAllEuDetailsController.onPageLoad(waypoints).url

  "DeleteAllEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints)(request, messages(application)).toString
      }
    }
    
    "must delete all EU registrations and redirect to the next page when the user answers Yes" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = answers
          .set(DeleteAllEuDetailsPage, true).success.value
          .remove(AllEuDetailsQuery).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteAllEuDetailsPage.navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not delete all EU registrations and then redirect to the next page when the user answers No" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value
        val expectedAnswers = answers
          .set(DeleteAllEuDetailsPage, false).success.value
          .set(TaxRegisteredInEuPage, true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteAllEuDetailsPage.navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
