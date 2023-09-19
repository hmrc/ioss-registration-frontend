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

import models.{Index, UserAnswers}
import pages.{AddItemPage, JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousRegistration.DeriveNumberOfPreviousSchemes

case class CheckPreviousSchemeAnswersPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: CheckPreviousSchemeAnswersPage => true
    case _ => false
  }
  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkPreviousSchemeAnswers"

  override val normalModeUrlFragment: String = "previous-scheme-answers"
  override val checkModeUrlFragment: String = "change-previous-scheme-answers"
  override def route(waypoints: Waypoints): Call =
    controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, Index(0))

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    (answers.get(this), answers.get(DeriveNumberOfPreviousSchemes(Index(0)))) match {
      case (Some(true), Some(size)) => PreviousSchemePage(Index(1), Index(size))
      case (Some(false), _) => AddPreviousRegistrationPage()
      case _ => JourneyRecoveryPage
    }
  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfPreviousSchemes(Index(0))
}
