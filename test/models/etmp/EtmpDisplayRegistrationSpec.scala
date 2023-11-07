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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}
import testutils.RegistrationData.etmpDisplayRegistration

class EtmpDisplayRegistrationSpec extends SpecBase {

  private val tradingNames: Seq[EtmpTradingName] = etmpDisplayRegistration.tradingNames
  private val schemeDetails: EtmpSchemeDetails = etmpDisplayRegistration.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpDisplayRegistration.bankDetails
  private val exclusions: Seq[EtmpExclusion] = etmpDisplayRegistration.exclusions
  private val adminUse: EtmpAdminUse = etmpDisplayRegistration.adminUse

  "EtmpDisplayRegistration" - {

    "must serialise/deserialise to and from EtmpDisplayRegistration" in {

      val displayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration

      val expectedJson = Json.obj(
          "tradingNames" -> tradingNames,
          "schemeDetails" -> schemeDetails,
          "bankDetails" -> bankDetails,
          "exclusions" -> exclusions,
          "adminUse" -> adminUse
      )

      Json.toJson(displayRegistration) mustBe expectedJson
      expectedJson.validate[EtmpDisplayRegistration] mustBe JsSuccess(displayRegistration)
    }
  }
}
