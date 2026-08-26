/*
 * Copyright 2026 HM Revenue & Customs
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

package models.intermediaries

import base.SpecBase
import models.etmp.EtmpExclusion
import play.api.libs.json.{JsError, JsSuccess, Json}


class EtmpDisplayRegistrationSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  "EtmpDisplayRegistration" - {

    "must deserialise/serialise from and to EtmpDisplayRegistration" in {

      val json = Json.obj(
        "customerIdentification" -> etmpDisplayRegistration.customerIdentification,
        "tradingNames" -> etmpDisplayRegistration.tradingNames,
        "clientDetails" -> etmpDisplayRegistration.clientDetails,
        "intermediaryDetails" -> etmpDisplayRegistration.intermediaryDetails,
        "otherAddress" -> etmpDisplayRegistration.otherAddress,
        "schemeDetails" -> etmpDisplayRegistration.schemeDetails,
        "exclusions" -> etmpDisplayRegistration.exclusions,
        "bankDetails" -> etmpDisplayRegistration.bankDetails,
        "adminUse" -> etmpDisplayRegistration.adminUse
      )

      val expectedResult = EtmpDisplayRegistration(
        customerIdentification = etmpDisplayRegistration.customerIdentification,
        tradingNames = etmpDisplayRegistration.tradingNames,
        clientDetails = etmpDisplayRegistration.clientDetails,
        intermediaryDetails = etmpDisplayRegistration.intermediaryDetails,
        otherAddress = etmpDisplayRegistration.otherAddress,
        schemeDetails = etmpDisplayRegistration.schemeDetails,
        exclusions = etmpDisplayRegistration.exclusions,
        bankDetails = etmpDisplayRegistration.bankDetails,
        adminUse = etmpDisplayRegistration.adminUse
      )

      json.validate[EtmpDisplayRegistration] `mustBe` JsSuccess(expectedResult)
      Json.toJson(expectedResult) `mustBe` json
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "tradingNames" -> 123456
      )

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }
  }
}
