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
import forms.euDetails.EuCountryFormProvider
import models.{Country, Index}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails.{EuCountryPage, HasFixedEstablishmentPage, TaxRegisteredInEuPage}
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuCountryView

class EuCountryControllerSpec extends SpecBase with MockitoSugar {
  
  private val waypoints: Waypoints = EmptyWaypoints
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val countryIndex: Index = Index(0)

  val formProvider = new EuCountryFormProvider()
  val form: Form[Country] = formProvider(countryIndex, Seq.empty)

  lazy val euCountryRoute: String = routes.EuCountryController.onPageLoad(waypoints, countryIndex).url

  "EuCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, countryIndex)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = basicUserAnswersWithVatInfo.set(EuCountryPage(countryIndex), country).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(country), waypoints, countryIndex)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, euCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value

        val expectedAnswers = basicUserAnswersWithVatInfo.set(EuCountryPage(countryIndex), country).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe EuCountryPage(countryIndex).navigate(waypoints, basicUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, euCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, countryIndex)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and question has been answered" in {

      val userAnswers = basicUserAnswersWithVatInfo
        .set(TaxRegisteredInEuPage, true).success.value
        .set(EuCountryPage(countryIndex), country).success.value
        .set(HasFixedEstablishmentPage(countryIndex), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, countryIndex)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euCountryRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
