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

package models.etmp.amend

import base.SpecBase
import play.api.libs.json._

import java.time.LocalDateTime


class AmendRegistrationResponseSpec extends SpecBase {

  "AmendRegistrationResponse" - {

    "must serialize to JSON correctly" in {
      val fixedDateTime = LocalDateTime.of(2025, 1, 17, 14, 32, 10, 686099000)
      val amendRegistrationResponse = AmendRegistrationResponse(
        processingDateTime = fixedDateTime,
        formBundleNumber = "12345",
        vrn = "123456789",
        iossReference = "IM900100000001",
        businessPartner = "businessPartner"
      )

      val expectedJson = Json.obj(
        "processingDateTime" -> s"2025-01-17T14:32:10.686099",
        "businessPartner" -> "businessPartner",
        "iossReference" -> "IM900100000001",
        "formBundleNumber" -> "12345",
        "vrn" -> "123456789"
      )

      Json.toJson(amendRegistrationResponse) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "processingDateTime" -> s"2025-01-17T14:32:10.686099",
        "businessPartner" -> "businessPartner",
        "iossReference" -> "IM900100000001",
        "formBundleNumber" -> "12345",
        "vrn" -> "123456789"
      )

      val fixedDateTime = LocalDateTime.of(2025, 1, 17, 14, 32, 10, 686099000)
      val amendRegistrationResponse = AmendRegistrationResponse(
        processingDateTime = fixedDateTime,
        formBundleNumber = "12345",
        vrn = "123456789",
        iossReference = "IM900100000001",
        businessPartner = "businessPartner"
      )

      json.validate[AmendRegistrationResponse] mustBe JsSuccess(amendRegistrationResponse)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[AmendRegistrationResponse] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "processingDateTime" -> s"2025-01-17T14:32:10.686099",
        "businessPartner" -> 12345,
        "iossReference" -> "IM900100000001",
        "formBundleNumber" -> "12345",
        "vrn" -> "123456789"
      )

      json.validate[AmendRegistrationResponse] mustBe a[JsError]
    }
  }

}

