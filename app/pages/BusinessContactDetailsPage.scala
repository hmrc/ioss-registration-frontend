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

package pages

import controllers.routes
import models.{BusinessContactDetails, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import utils.AmendWaypoints.AmendWaypointsOps

case object BusinessContactDetailsPage extends QuestionPage[BusinessContactDetails] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessContactDetails"

  override def route(waypoints: Waypoints): Call =
    routes.BusinessContactDetailsController.onPageLoad(waypoints)

  override def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    BankDetailsPage

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    answers.get(this) match {
      case Some(_) => waypoints.getNextCheckYourAnswersPageFromWaypoints.getOrElse(CheckYourAnswersPage)
      case _ => JourneyRecoveryPage
    }
}
