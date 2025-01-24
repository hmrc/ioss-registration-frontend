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

package models.previousRegistrations

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}

class NonCompliantDetailsSpec extends AnyFreeSpec with Matchers {

  "NonCompliantDetails" - {

    "must serialize to JSON" in {
      val nonCompliantDetails = NonCompliantDetails(Some(5), Some(10))
      val expectedJson = Json.obj(
        "nonCompliantReturns" -> 5,
        "nonCompliantPayments" -> 10
      )

      Json.toJson(nonCompliantDetails) mustBe expectedJson
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "nonCompliantReturns" -> 5,
        "nonCompliantPayments" -> 10
      )

      val expectedNonCompliantDetails = NonCompliantDetails(Some(5), Some(10))

      json.validate[NonCompliantDetails] mustBe JsSuccess(expectedNonCompliantDetails)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj(
        "nonCompliantReturns" -> 5
      )

      val expectedNonCompliantDetails = NonCompliantDetails(Some(5), None)

      json.validate[NonCompliantDetails] mustBe JsSuccess(expectedNonCompliantDetails)
    }

    "must handle all fields missing during deserialization" in {
      val json = Json.obj()

      val expectedNonCompliantDetails = NonCompliantDetails(None, None)

      json.validate[NonCompliantDetails] mustBe JsSuccess(expectedNonCompliantDetails)
    }

    "must return an error for invalid JSON structure" in {
      val invalidJson = Json.obj(
        "nonCompliantReturns" -> "invalidNumber",
        "nonCompliantPayments" -> 10
      )

      invalidJson.validate[NonCompliantDetails] mustBe a[JsError]
    }
  }
}
