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

package controllers.filters

import base.SpecBase
import forms.filters.RegisteredForVatInUkFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.filters.RegisteredForVatInUkPage
import pages.{EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UnauthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.filters.RegisteredForVatInUkView

class RegisteredForVatInUkControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints

  val formProvider = new RegisteredForVatInUkFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val registeredForVatInUkRoute: String = routes.RegisteredForVatInUkController.onPageLoad(waypoints).url

  "RegisteredForVatInUk Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registeredForVatInUkRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RegisteredForVatInUkView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(RegisteredForVatInUkPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registeredForVatInUkRoute)

        val view = application.injector.instanceOf[RegisteredForVatInUkView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UnauthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[UnauthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, registeredForVatInUkRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(RegisteredForVatInUkPage, true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RegisteredForVatInUkPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).route.url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, registeredForVatInUkRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RegisteredForVatInUkView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Registered for IOSS in Eu for a GET when there are no user answers" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, registeredForVatInUkRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.RegisteredForIossInEuController.onPageLoad(waypoints).url
      }
    }

    "must redirect to Registered for IOSS in Eu for a POST when there are no user answers" in {

      val mockSessionRepository = mock[UnauthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[UnauthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, registeredForVatInUkRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.RegisteredForIossInEuController.onPageLoad(waypoints).url
      }
    }
  }
}
