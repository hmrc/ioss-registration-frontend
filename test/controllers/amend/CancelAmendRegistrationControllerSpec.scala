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

package controllers.amend

import base.SpecBase
import config.FrontendAppConfig
import forms.amend.CancelAmendRegFormProvider
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{EmptyWaypoints, Waypoints}
import pages.amend.AmendYourAnswersPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.http.SessionKeys
import views.html.amend.CancelAmendRegistrationView

import scala.concurrent.Future

class CancelAmendRegistrationControllerSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints
  private val formProvider = new CancelAmendRegFormProvider
  private val form = formProvider()
  private lazy val CancelAmendRoute = routes.CancelAmendRegistrationController.onPageLoad().url

  "CancelAmendRegistration Controller" - {

    "must return OK and the correct view for a GET" in {


      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .build()

      running(application) {
        val request = FakeRequest(GET, CancelAmendRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelAmendRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must delete the amended answers and redirect to yourAccount page when the user answers Yes" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val sessionId = "12345-credId"
        val request =
          FakeRequest(POST, CancelAmendRoute)
            .withSession(SessionKeys.sessionId -> sessionId)
            .withFormUrlEncodedBody(("value", "true"))

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.iossYourAccountUrl
        verify(mockSessionRepository, times(1)).clear(eqTo(sessionId))
      }
    }

    "must NOT delete the amended answers and returns user to ChangeYourRegistration page when the user answers No" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val sessionId = "12345-credId"
        val request =
          FakeRequest(POST, CancelAmendRoute)
            .withSession(SessionKeys.sessionId -> sessionId)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual AmendYourAnswersPage.route(waypoints).url
        verify(mockSessionRepository, never()).set(eqTo(basicUserAnswersWithVatInfo))
      }
    }
  }
}
