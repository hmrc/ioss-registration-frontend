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

package pages.tradingNames

import controllers.tradingNames.routes
import models.{Index, UserAnswers}
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.tradingNames.AllTradingNames
import utils.AmendWaypoints.AmendWaypointsOps

case object HasTradingNamePage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "hasTradingName"

  override def route(waypoints: Waypoints): Call = routes.HasTradingNameController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true => TradingNamePage(Index(0))
      case false => PreviouslyRegisteredPage
    }.orRecover

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    (answers.get(this), answers.get(AllTradingNames)) match {
      case (Some(true), Some(tradingNames)) if tradingNames.nonEmpty => AddTradingNamePage()
      case (Some(true), _) => TradingNamePage(Index(0))
      case (Some(false), Some(tradingNames)) if tradingNames.nonEmpty => DeleteAllTradingNamesPage
      case (Some(false), _)  => waypoints.getNextCheckYourAnswersPageFromWaypoints.getOrElse(PreviouslyRegisteredPage)
      case _ => JourneyRecoveryPage
    }
  }
}
