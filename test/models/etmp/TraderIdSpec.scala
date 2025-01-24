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
import play.api.libs.json.{JsError, JsSuccess, Json}

class TraderIdSpec extends SpecBase {

  "TraderId" - {

    "must serialize and deserialize VatNumberTraderId correctly" in {
      val vatNumberTraderId = VatNumberTraderId("DE123456789")

      val expectedJson = Json.obj(
        "vatNumber" -> "DE123456789"
      )

      Json.toJson(vatNumberTraderId: TraderId) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "must serialize and deserialize TaxRefTraderID correctly" in {
      val taxRefTraderID = TaxRefTraderID("123456789")

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> "123456789"
      )

      Json.toJson(taxRefTraderID: TraderId) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[TraderId] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val expectedJson = Json.obj(
        "taxReferenceNumber" -> 1234567
      )

      expectedJson.validate[TraderId] mustBe a[JsError]
    }
  }

  "VatNumberTraderId" - {

    "must serialize to JSON correctly" in {
      val vatNumberTraderId = VatNumberTraderId("DE123456789")

      val expectedJson = Json.obj(
        "vatNumber" -> "DE123456789"
      )

      Json.toJson(vatNumberTraderId) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "vatNumber" -> "DE123456789"
      )

      json.validate[VatNumberTraderId] mustBe JsSuccess(VatNumberTraderId("DE123456789"))
    }

    "must fail deserialization with invalid data" in {
      val json = Json.obj(
        "vatNumber" -> 123456789
      )

      json.validate[VatNumberTraderId] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[VatNumberTraderId] mustBe a[JsError]
    }
  }

  "TaxRefTraderID" - {

    "must serialize to JSON correctly" in {
      val taxRefTraderID = TaxRefTraderID("123456789")

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> "123456789"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "taxReferenceNumber" -> "123456789"
      )

      json.validate[TaxRefTraderID] mustBe JsSuccess(TaxRefTraderID("123456789"))
    }

    "must fail deserialization with invalid data" in {
      val json = Json.obj(
        "taxReferenceNumber" -> 123456789
      )

      json.validate[TaxRefTraderID] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[TaxRefTraderID] mustBe a[JsError]
    }
  }
}

