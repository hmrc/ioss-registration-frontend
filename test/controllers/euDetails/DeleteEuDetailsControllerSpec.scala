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
import forms.euDetails.DeleteEuDetailsFormProvider
import models.euDetails.RegistrationType
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.DeleteEuDetailsView

class DeleteEuDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value

  val formProvider = new DeleteEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider(country)

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.TaxId).success.value
    .set(EuTaxReferencePage(countryIndex), arbitraryEuTaxReference.sample.value).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), "Trading name").success.value
    .set(FixedEstablishmentAddressPage(countryIndex), arbitraryInternationalAddress.arbitrary.sample.value).success.value

  lazy val deleteEuDetailsRoute: String = routes.DeleteEuDetailsController.onPageLoad(waypoints, countryIndex).url

  "DeleteEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteEuDetailsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }
    
    "must delete a record and then redirect to the next page when the user answers Yes" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = answers.remove(EuDetailsQuery(countryIndex)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          DeleteEuDetailsPage(countryIndex).navigate(waypoints, basicUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not delete a record and then redirect to the next page when the user answers No" in {

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
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          DeleteEuDetailsPage(countryIndex).navigate(waypoints, answers, answers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteEuDetailsView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteEuDetailsRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if the EU registration is not found" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, deleteEuDetailsRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
