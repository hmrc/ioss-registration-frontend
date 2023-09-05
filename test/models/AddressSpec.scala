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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class AddressSpec extends AnyFreeSpec with Matchers {

  "Address" - {

    "must serialise / deserialise from and to a DES address" - {

      "with all optional fields present" in {

        val address: Address = DesAddress("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("line 5"), Some("postcode"), "ES")

        val expectedJson = Json.obj(
          "line1" -> "line 1",
          "line2" -> "line 2",
          "line3" -> "line 3",
          "line4" -> "line 4",
          "line5" -> "line 5",
          "postCode" -> "postcode",
          "countryCode" -> "ES"
        )

        Json.toJson(address) mustEqual expectedJson
        expectedJson.validate[Address] mustEqual JsSuccess(address)
      }

      "with all optional fields missing" in {

        val address: Address = DesAddress("line 1", None, None, None, None, None, "FR")

        val expectedJson = Json.obj(
          "line1" -> "line 1",
          "countryCode" -> "FR"
        )

        Json.toJson(address) mustEqual expectedJson
        expectedJson.validate[Address] mustEqual JsSuccess(address)
      }

      "excluding trailing and leading whitespace and double spaces" in {
        val expectedJson = Json.obj(
          "line1" -> "  line   1",
          "line2" -> " line     2",
          "line3" -> " line    3  ",
          "line4" -> "  line   4 ",
          "line5" -> "       line    5  ",
          "postCode" -> "postcode",
          "countryCode" -> "DE"
        )

        expectedJson.as[DesAddress] mustEqual DesAddress(
          "line 1",
          Some("line 2"),
          Some("line 3"),
          Some("line 4"),
          Some("line 5"),
          Some("postcode"),
          "DE"
        )
      }
    }
  }
}
