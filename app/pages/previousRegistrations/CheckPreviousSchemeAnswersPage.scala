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
import pages.{CheckAnswersPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoint, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.previousRegistration.DeriveNumberOfPreviousSchemes

final case class CheckPreviousSchemeAnswersPage(index: Index) extends QuestionPage[Boolean]  with CheckAnswersPage  {


  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckPreviousSchemeAnswersPage => p.index == this.index
    case _ => false
  }
  override def path: JsPath = JsPath \ toString

  override val urlFragment: String = s"check-previous-scheme-answers-${index.display}"

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

object CheckPreviousSchemeAnswersPage {

  def waypointFromString(s: String): Option[Waypoint] = {

    val pattern = """check-previous-scheme-answers-(\d{1,3})""".r.anchored

    s match {
      case pattern(indexDisplay) =>
        Some(CheckPreviousSchemeAnswersPage(Index(indexDisplay.toInt - 1)).waypoint)
      case _ => None
    }
  }

}