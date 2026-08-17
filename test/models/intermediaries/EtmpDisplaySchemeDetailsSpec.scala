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

class EtmpDisplaySchemeDetailsSpec extends SpecBase {

  private val etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails =
    arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

  "EtmpDisplaySchemeDetails" - {

    "must deserialise/serialise from and to EtmpDisplaySchemeDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "commencementDate" -> etmpDisplaySchemeDetails.commencementDate,
          "euRegistrationDetails" -> etmpDisplaySchemeDetails.euRegistrationDetails,
          "contactName" -> etmpDisplaySchemeDetails.contactName,
          "businessTelephoneNumber" -> etmpDisplaySchemeDetails.businessTelephoneNumber,
          "businessEmailId" -> etmpDisplaySchemeDetails.businessEmailId,
          "unusableStatus" -> etmpDisplaySchemeDetails.unusableStatus,
          "nonCompliantReturns" -> etmpDisplaySchemeDetails.nonCompliantReturns,
          "nonCompliantPayments" -> etmpDisplaySchemeDetails.nonCompliantPayments
        )

        val expectedResult = EtmpDisplaySchemeDetails(
          commencementDate = etmpDisplaySchemeDetails.commencementDate,
          euRegistrationDetails = etmpDisplaySchemeDetails.euRegistrationDetails,
          contactName = etmpDisplaySchemeDetails.contactName,
          businessTelephoneNumber = etmpDisplaySchemeDetails.businessTelephoneNumber,
          businessEmailId = etmpDisplaySchemeDetails.businessEmailId,
          unusableStatus = etmpDisplaySchemeDetails.unusableStatus,
          nonCompliantReturns = etmpDisplaySchemeDetails.nonCompliantReturns,
          nonCompliantPayments = etmpDisplaySchemeDetails.nonCompliantPayments
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpDisplaySchemeDetails] `mustBe` JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "commencementDate" -> etmpDisplaySchemeDetails.commencementDate,
          "euRegistrationDetails" -> etmpDisplaySchemeDetails.euRegistrationDetails,
          "contactName" -> etmpDisplaySchemeDetails.contactName,
          "businessTelephoneNumber" -> etmpDisplaySchemeDetails.businessTelephoneNumber,
          "businessEmailId" -> etmpDisplaySchemeDetails.businessEmailId,
          "unusableStatus" -> etmpDisplaySchemeDetails.unusableStatus
        )

        val expectedResult = EtmpDisplaySchemeDetails(
          commencementDate = etmpDisplaySchemeDetails.commencementDate,
          euRegistrationDetails = etmpDisplaySchemeDetails.euRegistrationDetails,
          contactName = etmpDisplaySchemeDetails.contactName,
          businessTelephoneNumber = etmpDisplaySchemeDetails.businessTelephoneNumber,
          businessEmailId = etmpDisplaySchemeDetails.businessEmailId,
          unusableStatus = etmpDisplaySchemeDetails.unusableStatus,
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpDisplaySchemeDetails] `mustBe` JsSuccess(expectedResult)
      }
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpDisplaySchemeDetails] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "contactName" -> 123456
      )

      json.validate[EtmpDisplaySchemeDetails] `mustBe` a[JsError]
    }
  }
}
