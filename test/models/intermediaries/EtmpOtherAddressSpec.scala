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
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpOtherAddressSpec extends SpecBase {

  private val etmpOtherAddress: EtmpOtherAddress = arbitraryEtmpOtherAddress.arbitrary.sample.value

  "EtmpOtherAddress" - {

    "must deserialise/serialise from and to EtmpOtherAddress" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "issuedBy" -> etmpOtherAddress.issuedBy,
          "tradingName" -> etmpOtherAddress.tradingName,
          "addressLine1" -> etmpOtherAddress.addressLine1,
          "addressLine2" -> etmpOtherAddress.addressLine2,
          "townOrCity" -> etmpOtherAddress.townOrCity,
          "regionOrState" -> etmpOtherAddress.regionOrState,
          "postcode" -> etmpOtherAddress.postcode
        )

        val expectedResult: EtmpOtherAddress = EtmpOtherAddress(
          issuedBy = etmpOtherAddress.issuedBy,
          tradingName = etmpOtherAddress.tradingName,
          addressLine1 = etmpOtherAddress.addressLine1,
          addressLine2 = etmpOtherAddress.addressLine2,
          townOrCity = etmpOtherAddress.townOrCity,
          regionOrState = etmpOtherAddress.regionOrState,
          postcode = etmpOtherAddress.postcode
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpOtherAddress] `mustBe` JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "issuedBy" -> etmpOtherAddress.issuedBy,
          "addressLine1" -> etmpOtherAddress.addressLine1,
          "townOrCity" -> etmpOtherAddress.townOrCity
        )

        val expectedResult: EtmpOtherAddress = EtmpOtherAddress(
          issuedBy = etmpOtherAddress.issuedBy,
          tradingName = None,
          addressLine1 = etmpOtherAddress.addressLine1,
          addressLine2 = None,
          townOrCity = etmpOtherAddress.townOrCity,
          regionOrState = None,
          postcode = None
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpOtherAddress] `mustBe` JsSuccess(expectedResult)
      }
    }


    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpOtherAddress] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "issuedBy" -> 123456
      )

      json.validate[EtmpOtherAddress] `mustBe` a[JsError]
    }
  }
}
