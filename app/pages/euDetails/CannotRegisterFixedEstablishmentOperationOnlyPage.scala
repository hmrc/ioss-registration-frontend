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
import pages.{Page, Waypoints, RecoveryOps}
import play.api.mvc.Call
import queries.euDetails.AllEuDetailsQuery

case class CannotRegisterFixedEstablishmentOperationOnlyPage(countryIndex: Index) extends Page {

  override def route(waypoints: Waypoints): Call =
    routes.CannotRegisterFixedEstablishmentOperationOnlyController.onPageLoad(waypoints, countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(AllEuDetailsQuery).map {
      case n if n.isEmpty => TaxRegisteredInEuPage
      case n if n.nonEmpty => AddEuDetailsPage(Some(countryIndex))
      case _ => TaxRegisteredInEuPage
    }.orRecover
  }
}
