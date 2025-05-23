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

package pages.checkVatDetails

import controllers.checkVatDetails.routes
import models.CheckVatDetails.{DetailsIncorrect, WrongAccount, Yes}
import models.{CheckVatDetails, UserAnswers}
import pages.tradingNames.{AddTradingNamePage, HasTradingNamePage}
import pages.{JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.tradingNames.AllTradingNames

case object CheckVatDetailsPage extends QuestionPage[CheckVatDetails] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkVatDetails"

  override def route(waypoints: Waypoints): Call =
    routes.CheckVatDetailsController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    (answers.get(this), answers.vatInfo) match {
      case (Some(Yes), Some(vatInfo)) if vatInfo.desAddress.line1.nonEmpty =>
        if (answers.get(AllTradingNames).exists(_.nonEmpty)) {
          AddTradingNamePage()
        } else {
          HasTradingNamePage
        }
      case (Some(DetailsIncorrect), _)                                     => UpdateVatDetailsPage
      case (Some(WrongAccount), _)                                         => UseOtherAccountPage
      case _                                                               => JourneyRecoveryPage
    }
}
