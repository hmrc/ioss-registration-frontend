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

package pages.previousRegistrations

import controllers.previousRegistrations.routes
import models.{Index, UserAnswers}
import pages.euDetails.TaxRegisteredInEuPage
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoints, RecoveryOps}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.previousRegistration.DeriveNumberOfPreviousRegistrations
import utils.AmendWaypoints.AmendWaypointsOps

case object PreviouslyRegisteredPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "previouslyRegistered"

  override def route(waypoints: Waypoints): Call = routes.PreviouslyRegisteredController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true => PreviousEuCountryPage(Index(0))
      case false => TaxRegisteredInEuPage
    }.orRecover

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
  (answers.get(this), answers.get(DeriveNumberOfPreviousRegistrations)) match {
      case (Some(true), Some(size)) if size > 0   => AddPreviousRegistrationPage()
      case (Some(true), _)                        => PreviousEuCountryPage(Index(0))
      case (Some(false), Some(size)) if size > 0  => DeleteAllPreviousRegistrationsPage
      case (Some(false), _)  => waypoints.getNextCheckYourAnswersPageFromWaypoints.getOrElse(CheckYourAnswersPage)
      case _ => JourneyRecoveryPage
    }
}
