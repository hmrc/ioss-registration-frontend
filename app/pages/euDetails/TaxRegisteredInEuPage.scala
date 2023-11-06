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

package pages.euDetails

import controllers.euDetails.routes
import models.{Index, UserAnswers}
import pages.amend.AmendYourAnswersPage
import pages.website.WebsitePage
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.euDetails.AllEuDetailsQuery
import utils.AmendWaypoints.AmendWaypointsOps

case object TaxRegisteredInEuPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "taxRegisteredInEu"

  override def route(waypoints: Waypoints): Call =
    routes.TaxRegisteredInEuController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true => EuCountryPage(Index(0))
      case false => WebsitePage(Index(0))
    }.orRecover

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    (answers.get(this), answers.get(AllEuDetailsQuery)) match {
      case (Some(true), Some(euDetails)) if euDetails.nonEmpty => AddEuDetailsPage()
      case (Some(true), _) => EuCountryPage(Index(0))
      case (Some(false), Some(euDetails)) if euDetails.nonEmpty => DeleteAllEuDetailsPage
      case (Some(false), _) if waypoints.inAmend => AmendYourAnswersPage
      case (Some(false), _) => CheckYourAnswersPage
      case _ => JourneyRecoveryPage
    }
}
