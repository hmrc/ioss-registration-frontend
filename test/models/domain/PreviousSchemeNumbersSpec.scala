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

package models.domain

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsError, JsSuccess, Json}

class PreviousSchemeNumbersSpec extends SpecBase with ScalaFutures {

  "PreviousSchemeNumbers" - {

    "must serialize to JSON correctly" in {
      val previousSchemeNumbers = PreviousSchemeNumbers("EU123456789", None)

      val expectedJson = Json.obj(
        "previousSchemeNumber" -> "EU123456789"
      )

      Json.toJson(previousSchemeNumbers) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "previousSchemeNumber" -> "EU123456789"
      )

      val previousSchemeNumbers = PreviousSchemeNumbers("EU123456789", None)

      json.validate[PreviousSchemeNumbers] mustBe JsSuccess(previousSchemeNumbers)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[PreviousSchemeNumbers] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "previousSchemeNumber" -> 123456789
      )

      json.validate[PreviousSchemeNumbers] mustBe a[JsError]
    }
  }
}
