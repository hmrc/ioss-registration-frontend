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

package models.ossRegistration

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.*

class OssContactDetailsSpec extends SpecBase {

  "OssContactDetails" - {

    "must serialise and deserialise to / from OssContactDetails" in {

      val json = Json.obj(
        "fullName" -> "Test Contact",
        "telephoneNumber" -> "123456789",
        "emailAddress" -> "test@example.com"
      )

      val expectedResult = OssContactDetails(
        fullName = "Test Contact",
        telephoneNumber = "123456789",
        emailAddress = "test@example.com"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssContactDetails] mustBe JsSuccess(expectedResult)

    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "fullName" -> "Test Contact",
        "telephoneNumber" -> 123456789,
        "emailAddress" -> "test@example.com"
      )

      json.validate[OssContactDetails] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "fullName" -> "Test Contact",
        "telephoneNumber" -> JsNull,
        "emailAddress" -> "test@example.com"
      )

      json.validate[OssContactDetails] mustBe a[JsError]
    }

    "must handle missing data during deserialization" in {

      val json = Json.obj()

      json.validate[OssContactDetails] mustBe a[JsError]
    }
  }
}
