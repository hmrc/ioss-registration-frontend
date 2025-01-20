/*
 * Copyright 2024 HM Revenue & Customs
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

package models.external

import base.SpecBase
import play.api.libs.json.{JsValue, Json}


class ExternalResponseSpec extends SpecBase {

  "ExternalResponse" - {

    "serialize correctly to JSON" in {

      val externalResponse = ExternalResponse("http://example.com")
      val json: JsValue = Json.toJson(externalResponse)

      json mustBe Json.parse("""{"redirectUrl":"http://example.com"}""")

    }

    "deserialize correctly from JSON" in {

      val json: JsValue = Json.parse("""{"redirectUrl":"http://example.com"}""")
      val externalResponse = json.as[ExternalResponse]

      externalResponse mustBe ExternalResponse("http://example.com")
    }

    "handle missing redirectUrl during deserialization" in {

      val json: JsValue = Json.parse("""{}""")

      intercept[Exception] {
        json.as[ExternalResponse]
      }

    }

    "handle extra fields gracefully during deserialization" in {

      val json: JsValue = Json.parse(
        """{"redirectUrl":"http://example.com", "extraField":"extraValue"}"""
      )

      val externalResponse = json.as[ExternalResponse]

      externalResponse mustBe ExternalResponse("http://example.com")

    }
  }
}

