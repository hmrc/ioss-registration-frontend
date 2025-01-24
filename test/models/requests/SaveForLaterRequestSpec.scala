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

package models.requests

import base.SpecBase
import models.DesAddress
import models.domain.VatCustomerInfo
import play.api.libs.json.Json

import java.time.LocalDate

class SaveForLaterRequestSpec extends SpecBase {

  "SaveForLaterRequest" - {

    "serialize and deserialize correctly" in {

      val data = Json.obj("key" -> "value")
      val desAddress = DesAddress(
        line1 = "123 Example Street",
        line2 = None,
        line3 = None,
        line4 = None,
        line5 = None,
        postCode = Some("AB12 3CD"),
        countryCode = "GB"
      )

      val vatCustomerInfo = VatCustomerInfo(
        desAddress = desAddress,
        registrationDate = LocalDate.parse("2023-01-01"),
        partOfVatGroup = false,
        organisationName = Some("Example Ltd"),
        individualName = None,
        singleMarketIndicator = true,
        deregistrationDecisionDate = None,
        overseasIndicator = false
      )

      val saveForLaterRequest = SaveForLaterRequest(
        vrn = vrn,
        data = data,
        vatInfo = Some(vatCustomerInfo)
      )

      val json = Json.toJson(saveForLaterRequest)

      val expectedJson = Json.parse(
        s"""
        {
          "vrn": "123456789",
          "data": {
            "key": "value"
          },
          "vatInfo": {
            "desAddress": {
              "line1": "123 Example Street",
              "countryCode": "GB",
              "postCode": "AB12 3CD"
            },
            "registrationDate": "2023-01-01",
            "partOfVatGroup": false,
            "organisationName": "Example Ltd",
            "singleMarketIndicator": true,
            "overseasIndicator": false
          }
        }
        """
      )

      json mustBe expectedJson

      val deserialized = json.as[SaveForLaterRequest]
      deserialized mustBe saveForLaterRequest
      deserialized.vrn mustBe vrn
      deserialized.data mustBe data
      deserialized.vatInfo mustBe Some(vatCustomerInfo)
    }
  }
}

