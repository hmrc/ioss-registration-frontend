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
import pages.{AddItemPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousRegistration.DeriveNumberOfPreviousRegistrations

case class AddPreviousRegistrationPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddPreviousRegistrationPage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = "previous-schemes-overview"
  override val checkModeUrlFragment: String = "change-previous-schemes-overview"
  override def path: JsPath = JsPath \ toString

  override def toString: String = "addPreviousRegistration"

  override def route(waypoints: Waypoints): Call =
    routes.AddPreviousRegistrationController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    (answers.get(AddPreviousRegistrationPage()), answers.get(DeriveNumberOfPreviousRegistrations)) match {
      case (Some(true), Some(size)) => PreviousEuCountryPage(Index(size))
      case (Some(false), _) => TaxRegisteredInEuPage
      case _ => JourneyRecoveryPage
    }
  }


  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    (answers.get(AddPreviousRegistrationPage()), answers.get(DeriveNumberOfPreviousRegistrations)) match {
      case (Some(true), Some(size)) => PreviousEuCountryPage(Index(size))
      case (Some(false), _) => TaxRegisteredInEuPage
      case _ => JourneyRecoveryPage
    }
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfPreviousRegistrations

}
