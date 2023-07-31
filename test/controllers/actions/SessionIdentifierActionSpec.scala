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

package controllers.actions

import base.SpecBase
import play.api.mvc.{Action, AnyContent, DefaultActionBuilder, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext.Implicits.global

class SessionIdentifierActionSpec extends SpecBase {

  class Harness(identify: SessionIdentifierAction, actionBuilder: DefaultActionBuilder) {
    def onPageLoad(): Action[AnyContent] = (actionBuilder andThen identify) { _ => Results.Ok }
  }

  "Session Action" - {

    "when there is no active session" - {

      "must redirect to the journey recovery expired page" in {

        val application = applicationBuilder(userAnswers = None).build()

        def actionBuilder: DefaultActionBuilder = application.injector.instanceOf[DefaultActionBuilder]

        val sessionAction = new SessionIdentifierAction()

        val controller = new Harness(sessionAction, actionBuilder)

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(controllers.routes.JourneyRecoveryController.onPageLoad().url)

        application.stop()
      }
    }

    "when there is an active session" - {

      "must perform the action" in {

        val application = applicationBuilder(userAnswers = None).build()

        def actionBuilder: DefaultActionBuilder = application.injector.instanceOf[DefaultActionBuilder]

        val sessionAction = new SessionIdentifierAction()

        val controller = new Harness(sessionAction, actionBuilder)

        val request = fakeRequest.withSession(SessionKeys.sessionId -> "foo")

        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        application.stop()
      }
    }
  }
}
