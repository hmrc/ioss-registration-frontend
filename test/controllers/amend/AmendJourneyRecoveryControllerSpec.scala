/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import testutils.WireMockHelper
import uk.gov.hmrc.http.SessionKeys
import views.html.amend.AmendJourneyRecoveryView

import scala.concurrent.Future

class AmendJourneyRecoveryControllerSpec extends SpecBase with WireMockHelper {

  "AmendJourneyRecovery Controller" - {

    "must delete all user answers and return OK and the correct view for a GET" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val sessionId = userAnswersId
        val request = FakeRequest(GET, routes.AmendJourneyRecoveryController.onPageLoad().url)
          .withSession(SessionKeys.sessionId -> sessionId)

        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendJourneyRecoveryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(frontendAppConfig.iossYourAccountUrl)(request, messages(application)).toString
        verify(mockSessionRepository, times(1)).clear(eqTo(sessionId))
      }
    }
  }
}
