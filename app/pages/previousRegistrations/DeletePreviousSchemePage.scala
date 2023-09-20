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
import pages.{Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.previousRegistration.{DeriveNumberOfPreviousRegistrations, DeriveNumberOfPreviousSchemes}

case class DeletePreviousSchemePage(countryIndex: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "deletePreviousScheme"

  override def route(waypoints: Waypoints): Call =
    controllers.previousRegistrations.routes.DeletePreviousRegistrationController.onPageLoad(waypoints, countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    (answers.get(DeriveNumberOfPreviousRegistrations), answers.get(DeriveNumberOfPreviousSchemes(countryIndex))) match {
      case (_, Some(numberOfSchemes)) if numberOfSchemes > 0 =>
        CheckPreviousSchemeAnswersPage(Some(countryIndex))
      case (Some(numberOfCountries), _) if numberOfCountries > 0 =>
        AddPreviousRegistrationPage()
      case _ =>
        PreviouslyRegisteredPage
    }
  }
}
