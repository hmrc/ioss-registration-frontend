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
import testutils.RegistrationData.etmpSchemeDetails

class EtmpSchemeDetailsSpec extends SpecBase {

  private val commencementDate = etmpSchemeDetails.commencementDate
  private val euRegistrationDetails = etmpSchemeDetails.euRegistrationDetails
  private val previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails
  private val websites = etmpSchemeDetails.websites
  private val contactName = etmpSchemeDetails.contactName
  private val businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber
  private val businessEmailId = etmpSchemeDetails.businessEmailId
  private val nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns
  private val nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments

  "EtmpSchemeDetails" - {

    "must deserialise/serialise to and from EtmpSchemeDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
          "nonCompliantReturns" -> nonCompliantReturns,
          "nonCompliantPayments" -> nonCompliantPayments
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpSchemeDetails] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "commencementDate" -> 12345,
        "euRegistrationDetails" -> euRegistrationDetails,
        "previousEURegistrationDetails" -> previousEURegistrationDetails,
        "websites" -> websites,
        "contactName" -> contactName,
        "businessTelephoneNumber" -> businessTelephoneNumber,
        "businessEmailId" -> businessEmailId,
      )

      json.validate[EtmpSchemeDetails] mustBe a[JsError]
    }
  }
}

