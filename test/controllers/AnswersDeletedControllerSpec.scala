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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UnauthenticatedUserAnswersRepository
import uk.gov.hmrc.http.SessionKeys
import utils.FutureSyntax.FutureOps
import views.html.AnswersDeletedView

class AnswersDeletedControllerSpec extends SpecBase with MockitoSugar {

  "AnswersDeleted Controller" - {

    "must delete user's answers and return OK and the correct view for a GET" in {

      val mockSessionRepository: UnauthenticatedUserAnswersRepository = mock[UnauthenticatedUserAnswersRepository]
      when(mockSessionRepository.clear(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[UnauthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val sessionId = "123"
        val request = FakeRequest(GET, routes.AnswersDeletedController.onPageLoad().url)
          .withSession(SessionKeys.sessionId -> sessionId)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AnswersDeletedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
        verify(mockSessionRepository, (times(1))).clear(eqTo(sessionId))
      }
    }
  }
}
