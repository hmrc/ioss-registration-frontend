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


import models.ossRegistration.OssEuTaxIdentifierType.Vat
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class OssEuTaxIdentifierSpec extends AnyFreeSpec with Matchers {

  "EuTaxIdentifier" - {

    "must deserialise/serialise to and from EuTaxIdentifier" in {

      val json = Json.obj(
        "identifierType" -> "vat",
        "value" -> "123456789"
      )

      val expectedResult = OssEuTaxIdentifier(
        identifierType = Vat,
        value = "123456789"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssEuTaxIdentifier] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssEuTaxIdentifier] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "identifierType" -> 12345,
        "value" -> "123456789"
      )

      json.validate[OssEuTaxIdentifier] mustBe a[JsError]

    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "identifierType" -> JsNull,
        "value" -> "123456789"
      )

      json.validate[OssEuTaxIdentifier] mustBe a[JsError]
    }
  }
}
