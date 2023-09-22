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
import models.{Index, NormalMode, UserAnswers}
import pages.{AddToListQuestionPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoint, Waypoints}
import play.api.libs.json.{JsPath}
import play.api.mvc.Call
import queries.previousRegistration.DeriveNumberOfPreviousSchemes

case class CheckPreviousSchemeAnswersPage(index: Index) extends QuestionPage[Boolean]  with AddToListQuestionPage  {


  override val addItemWaypoint: Waypoint = AddPreviousRegistrationPage().waypoint(NormalMode)
  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkPreviousSchemeAnswers"

  override def route(waypoints: Waypoints): Call =
    routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    (answers.get(CheckPreviousSchemeAnswersPage(index)), answers.get(DeriveNumberOfPreviousSchemes(index))) match {
      case (Some(true), Some(size)) => PreviousSchemePage(index, Index(size))
      case (Some(false), _) => AddPreviousRegistrationPage()
      case _ => JourneyRecoveryPage
    }

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    (answers.get(CheckPreviousSchemeAnswersPage(index)), answers.get(DeriveNumberOfPreviousSchemes(index))) match {
      case (Some(true), Some(size)) => PreviousSchemePage(index, Index(size))
      case (Some(false), _) => AddPreviousRegistrationPage()
      case _ => JourneyRecoveryPage
    }
  }

}
