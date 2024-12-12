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

package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.PreviousSchemeTypeFormProvider
import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousSchemeTypePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.previousRegistrations.PreviousSchemeView

import scala.concurrent.Future

class PreviousSchemeControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)

  private val country = Country.euCountries.head
  private val baseAnswers = emptyUserAnswers.set(PreviousEuCountryPage(index), country).success.value

  private val formProvider = new PreviousSchemeTypeFormProvider()
  private val form = formProvider(country.name, PreviousScheme.values, index)

  private val waypoints: Waypoints = EmptyWaypoints


  private lazy val previousSchemeRoute = controllers.previousRegistrations.routes.PreviousSchemeController.onPageLoad(waypoints, index, index).url

  "PreviousScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousSchemeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(PreviousSchemeTypePage(index, index), PreviousSchemeType.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousSchemeRoute)

        val view = application.injector.instanceOf[PreviousSchemeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(PreviousSchemeType.values.head), waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, previousSchemeRoute)
            .withFormUrlEncodedBody(("value", PreviousSchemeType.values.head.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(PreviousSchemeTypePage(index, index), PreviousSchemeType.values.head).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual PreviousSchemeTypePage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousSchemeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PreviousSchemeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousSchemeRoute)
            .withFormUrlEncodedBody(("value", PreviousSchemeType.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
