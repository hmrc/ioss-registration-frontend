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
import models.{CheckMode, Index, NormalMode, UserAnswers}
import pages.{AddItemPage, Page, QuestionPage, Waypoint, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousRegistration.DeriveNumberOfPreviousSchemes

case class CheckPreviousSchemeAnswersPage(countryIndex: Index, schemeIndex: Option[Index] = None) extends AddItemPage(schemeIndex) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckPreviousSchemeAnswersPage => p.countryIndex == this.countryIndex
    case _ => false
  }

  override val checkModeUrlFragment: String = s"change-previous-scheme-answers-${countryIndex.display}"
  override val normalModeUrlFragment: String = s"previous-scheme-answers-${countryIndex.display}"

  override def path: JsPath = JsPath \ "previousRegistrations" \ countryIndex.position \ toString

  override def toString: String = "checkPreviousSchemeAnswers"

  override def route(waypoints: Waypoints): Call =
    routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        schemeIndex
        .map(i => PreviousSchemePage(countryIndex, Index(i.position + 1)))
        .getOrElse {
          answers
            .get(deriveNumberOfItems)
            .map(n => PreviousSchemePage(countryIndex, Index(n)))
            .orRecover
        }

      case false =>
        AddPreviousRegistrationPage()
    }.orRecover

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfPreviousSchemes(countryIndex)
}

object CheckPreviousSchemeAnswersPage {

  def waypointFromString(s: String): Option[Waypoint] = {

    val normalModePattern = """previous-scheme-answers-(\d{1,3})""".r.anchored
    val checkModePattern = """change-previous-scheme-answers-(\d{1,3})""".r.anchored

    s match {
      case normalModePattern(indexDisplay) =>
        Some(CheckPreviousSchemeAnswersPage(Index(indexDisplay.toInt - 1), None).waypoint(NormalMode))

      case checkModePattern(indexDisplay) =>
        Some(CheckPreviousSchemeAnswersPage(Index(indexDisplay.toInt - 1), None).waypoint(CheckMode))

      case _ =>
        None
    }
  }
}
