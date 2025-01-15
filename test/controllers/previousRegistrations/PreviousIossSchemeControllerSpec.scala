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
import forms.previousRegistrations.PreviousIossSchemeFormProvider
import models.{Country, Index, PreviousScheme, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossSchemePage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.previousRegistrations.PreviousIossSchemeView

import scala.concurrent.Future

class PreviousIossSchemeControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints
  private val formProvider = new PreviousIossSchemeFormProvider()
  private val form = formProvider()

  private val country = Country.euCountries.head
  private val baseAnswers = emptyUserAnswers.set(PreviousEuCountryPage(index), country).success.value

  private lazy val previousIossSchemeRoute = controllers.previousRegistrations.routes.PreviousIossSchemeController.onPageLoad(waypoints, index, index).url

  "PreviousIossScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossSchemeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousIossSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(PreviousIossSchemePage(index, index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossSchemeRoute)

        val view = application.injector.instanceOf[PreviousIossSchemeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" - {
      "and also sets previous scheme as with intermediary when true" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossSchemeRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers
            .set(PreviousIossSchemePage(index, index), true).success.value
            .set(PreviousSchemePage(index, index), PreviousScheme.IOSSWI).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossSchemePage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "and also sets previous scheme as without intermediary when false" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossSchemeRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers
            .set(PreviousIossSchemePage(index, index), false).success.value
            .set(PreviousSchemePage(index, index), PreviousScheme.IOSSWOI).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossSchemePage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossSchemeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviousIossSchemeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIossSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossSchemeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
