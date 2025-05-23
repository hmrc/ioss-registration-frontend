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
import models.UserAnswers
import models.requests.AuthenticatedDataRequest
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.rejoin.RejoinRegistrationPage
import pages.{EmptyWaypoints, NonEmptyWaypoints, QuestionPage, Waypoint, Waypoints}
import play.api.libs.json.{Json, JsPath}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import play.api.test.FakeRequest
import queries.Gettable
import uk.gov.hmrc.auth.core.Enrolments
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

class AnswerExtractorSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints
  private val rejoinWaypoint: Waypoints = NonEmptyWaypoints(Waypoint.fromString(RejoinRegistrationPage.urlFragment).get, Nil)
  private val amendActiveWaypoints: Waypoints = NonEmptyWaypoints(Waypoint.fromString(ChangeRegistrationPage.urlFragment).get, Nil)
  private val amendPreviousWaypoints: Waypoints = NonEmptyWaypoints(Waypoint.fromString(ChangePreviousRegistrationPage.urlFragment).get, Nil)

  private object TestPage extends QuestionPage[Int] {
    override def path: JsPath = JsPath \ "test"

    override def route(waypoints: Waypoints): Call = Call("", "")
  }

  private def buildRequest(answers: UserAnswers): AuthenticatedDataRequest[AnyContent] =
    AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, answers, None, 1, None)

  private class TestController extends AnswerExtractor {

    def get(waypoints: Waypoints, query: Gettable[Int])(implicit request: AuthenticatedDataRequest[AnyContent]): Result =
      getAnswer(waypoints, query) {
        answer =>
          Ok(Json.toJson(answer))
      }

    def getAsync(waypoints: Waypoints, query: Gettable[Int])(implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
      getAnswerAsync(waypoints, query) {
        answer =>
          Ok(Json.toJson(answer)).toFuture
      }
  }

  "getAnswer" - {

    "must pass the answer into the provided block when the answer exists in user answers" in {

      val answers = emptyUserAnswers.set(TestPage, 1).success.value
      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(answers)

      val controller = new TestController()

      controller.get(waypoints, TestPage) mustBe Ok(Json.toJson(1))
    }

    "must redirect to Journey Recovery when the answer does not exist in user answers" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.get(waypoints, TestPage) mustBe Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

    "must redirect to RejoinRegistrationPage when the answer does not exist and waypoints are in RejoiningRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.get(rejoinWaypoint, TestPage) mustBe Redirect(RejoinRegistrationPage.route(rejoinWaypoint))
    }

    "must redirect to ChangeRegistrationPage when the answer does not exist and waypoints are in AmendingActiveRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.get(amendActiveWaypoints, TestPage) mustBe Redirect(ChangeRegistrationPage.route(amendActiveWaypoints))
    }

    "must redirect to ChangePreviousRegistrationPage when the answer does not exist and waypoints are in AmendingPreviousRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.get(amendPreviousWaypoints, TestPage) mustBe Redirect(ChangePreviousRegistrationPage.route(amendPreviousWaypoints))

    }
  }

  "getAnswerAsync" - {

    "must pass the answer into the provided block when the answer exists in user answers" in {

      val answers = emptyUserAnswers.set(TestPage, 1).success.value
      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(answers)

      val controller = new TestController()

      controller.getAsync(waypoints, TestPage).futureValue mustBe Ok(Json.toJson(1))
    }

    "must redirect to Journey Recovery when the answer does not exist in user answers" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.getAsync(waypoints, TestPage).futureValue mustBe Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

    "must redirect to RejoinRegistrationPage when the answer does not exist and waypoints are in RejoiningRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.getAsync(rejoinWaypoint, TestPage).futureValue mustBe Redirect(RejoinRegistrationPage.route(rejoinWaypoint))
    }

    "must redirect to ChangeRegistrationPage when the answer does not exist and waypoints are in AmendingActiveRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.getAsync(amendActiveWaypoints, TestPage).futureValue mustBe Redirect(ChangeRegistrationPage.route(amendActiveWaypoints))
    }

    "must redirect to ChangePreviousRegistrationPage when the answer does not exist and waypoints are in AmendingPreviousRegistration mode" in {

      implicit val request: AuthenticatedDataRequest[AnyContent] = buildRequest(emptyUserAnswers)

      val controller = new TestController()

      controller.getAsync(amendPreviousWaypoints, TestPage).futureValue mustBe Redirect(ChangePreviousRegistrationPage.route(amendPreviousWaypoints))

    }
  }
}
