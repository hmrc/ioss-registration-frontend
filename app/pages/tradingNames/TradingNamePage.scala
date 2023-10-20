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
import models.{Index, NormalMode, TradingName, UserAnswers}
import pages.{AddToListQuestionPage, Page, QuestionPage, Waypoint, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.tradingNames.AllTradingNames

import scala.util.Try

case class TradingNamePage(index: Index) extends QuestionPage[TradingName] with AddToListQuestionPage {

  override val addItemWaypoint: Waypoint = AddTradingNamePage().waypoint(NormalMode)

  override def path: JsPath = JsPath \ toString \ index.position

  override def toString: String = "tradingNames"

  override def route(waypoints: Waypoints): Call =
    routes.TradingNameController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    AddTradingNamePage(Some(index))

  override def cleanup(value: Option[TradingName], userAnswers: UserAnswers): Try[UserAnswers] = {
    if (userAnswers.get(AllTradingNames).exists(_.isEmpty)) {
      userAnswers.remove(AllTradingNames)
    } else {
      super.cleanup(value, userAnswers)
    }
  }
}