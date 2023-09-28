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
import models.euDetails.RegistrationType
import models.{Index, UserAnswers}
import pages.{JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.euDetails.DeriveNumberOfEuRegistrations

import scala.util.Try

case class RegistrationTypePage(countryIndex: Index) extends QuestionPage[RegistrationType] {

  override def path: JsPath = JsPath \ "euDetails" \ countryIndex.position \ toString

  override def toString: String = "registrationType"

  override def route(waypoints: Waypoints): Call =
    routes.RegistrationTypeController.onPageLoad(waypoints, countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case RegistrationType.VatNumber => EuVatNumberPage(countryIndex)
      case RegistrationType.TaxId => EuTaxReferencePage(countryIndex)
    }.orRecover

  override def cleanup(value: Option[RegistrationType], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(RegistrationType.VatNumber) =>
        userAnswers.remove(EuTaxReferencePage(countryIndex))
      case Some(RegistrationType.TaxId) =>
        userAnswers.remove(EuVatNumberPage(countryIndex))
      case None => super.cleanup(value, userAnswers)
    }
  }
}
