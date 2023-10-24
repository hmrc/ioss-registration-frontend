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
import testutils.RegistrationData.etmpRegistrationRequest

class EtmpRegistrationRequestSpec extends SpecBase {

  private val administration = etmpRegistrationRequest.administration
  private val customerIdentification = etmpRegistrationRequest.customerIdentification
  private val tradingNames = etmpRegistrationRequest.tradingNames
  private val schemeDetails = etmpRegistrationRequest.schemeDetails
  private val bankDetails = etmpRegistrationRequest.bankDetails


  "EtmpRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> administration,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )

      val expectedResult = EtmpRegistrationRequest(
        administration = administration,
        customerIdentification = customerIdentification,
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpRegistrationRequest] mustBe JsSuccess(expectedResult)
    }
  }
}

