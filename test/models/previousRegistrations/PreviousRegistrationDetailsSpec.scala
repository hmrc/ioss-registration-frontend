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

import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.etmp.EtmpPreviousEuRegistrationDetails
import models.{Country, PreviousScheme}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.*
import testutils.RegistrationData.etmpEuPreviousRegistrationDetails

class PreviousRegistrationDetailsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PreviousRegistrationDetails" - {

    "serialize to JSON correctly" in {
      val previousEuCountry = Country("FR", "France")
      val schemeNumbers = PreviousSchemeNumbers(
        previousSchemeNumber = "12345",
        previousIntermediaryNumber = Some("67890")
      )
      val schemeDetails = PreviousSchemeDetails(
        previousScheme = PreviousScheme.OSSU,
        previousSchemeNumbers = schemeNumbers,
        nonCompliantDetails = Some(NonCompliantDetails(nonCompliantReturns = Some(1), nonCompliantPayments = Some(1)))
      )
      val previousRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = previousEuCountry,
        previousSchemesDetails = Seq(schemeDetails)
      )

      val expectedJson: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "FR",
           |    "name": "France"
           |  },
           |  "previousSchemesDetails": [
           |    {
           |      "previousScheme": "ossu",
           |      "previousSchemeNumbers": {
           |        "previousSchemeNumber": "12345",
           |        "previousIntermediaryNumber": "67890"
           |      },
           |      "nonCompliantDetails": {
           |        "nonCompliantReturns":1,
           |		    "nonCompliantPayments":1
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      Json.toJson(previousRegistrationDetails) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "FR",
           |    "name": "France"
           |  },
           |  "previousSchemesDetails": [
           |    {
           |      "previousScheme": "ossu",
           |      "previousSchemeNumbers": {
           |        "previousSchemeNumber": "12345",
           |        "previousIntermediaryNumber": "67890"
           |      },
           |      "nonCompliantDetails": {
           |        "nonCompliantReturns":1,
           |		    "nonCompliantPayments":1
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = Country("FR", "France"),
        previousSchemesDetails = Seq(PreviousSchemeDetails(
          previousScheme = PreviousScheme.OSSU,
          previousSchemeNumbers = PreviousSchemeNumbers(
            previousSchemeNumber = "12345",
            previousIntermediaryNumber = Some("67890")
          ),
          nonCompliantDetails = Some(NonCompliantDetails(nonCompliantReturns = Some(1), nonCompliantPayments = Some(1)))
        ))
      )

      json.as[PreviousRegistrationDetails] mustBe expectedRegistrationDetails
    }

    "fail deserialization when required fields are missing" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousSchemesDetails": []
           |}
           |""".stripMargin
      )

      intercept[JsResultException] {
        json.as[PreviousRegistrationDetails]
      }
    }

    "serialize to JSON correctly when there are no schemes" in {
      val previousEuCountry = Country("DE", "Germany")
      val previousRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = previousEuCountry,
        previousSchemesDetails = Seq.empty
      )

      val expectedJson: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "DE",
           |    "name": "Germany"
           |  },
           |  "previousSchemesDetails": []
           |}
           |""".stripMargin
      )

      Json.toJson(previousRegistrationDetails) mustBe expectedJson
    }

    "deserialize from JSON correctly when there are no schemes" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "DE",
           |    "name": "Germany"
           |  },
           |  "previousSchemesDetails": []
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = Country("DE", "Germany"),
        previousSchemesDetails = Seq.empty
      )

      json.as[PreviousRegistrationDetails] mustBe expectedRegistrationDetails
    }

    "handle deserialization when non-compliant details are missing" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "FR",
           |    "name": "France"
           |  },
           |  "previousSchemesDetails": [
           |    {
           |      "previousScheme": "ossu",
           |      "previousSchemeNumbers": {
           |        "previousSchemeNumber": "12345",
           |        "previousIntermediaryNumber": "67890"
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = Country("FR", "France"),
        previousSchemesDetails = Seq(PreviousSchemeDetails(
          previousScheme = PreviousScheme.OSSU,
          previousSchemeNumbers = PreviousSchemeNumbers(
            previousSchemeNumber = "12345",
            previousIntermediaryNumber = Some("67890")
          ),
          nonCompliantDetails = None
        ))
      )

      json.as[PreviousRegistrationDetails] mustBe expectedRegistrationDetails
    }

    "handle deserialization when intermediary number is missing" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "IT",
           |    "name": "Italy"
           |  },
           |  "previousSchemesDetails": [
           |    {
           |      "previousScheme": "ossu",
           |      "previousSchemeNumbers": {
           |        "previousSchemeNumber": "12345"
           |      },
           |      "nonCompliantDetails": {
           |        "nonCompliantReturns": 2,
           |        "nonCompliantPayments": 3
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = Country("IT", "Italy"),
        previousSchemesDetails = Seq(PreviousSchemeDetails(
          previousScheme = PreviousScheme.OSSU,
          previousSchemeNumbers = PreviousSchemeNumbers(
            previousSchemeNumber = "12345",
            previousIntermediaryNumber = None
          ),
          nonCompliantDetails = Some(NonCompliantDetails(nonCompliantReturns = Some(2), nonCompliantPayments = Some(3)))
        ))
      )

      json.as[PreviousRegistrationDetails] mustBe expectedRegistrationDetails
    }

    "fromEtmpPreviousEuRegistrationDetails handles no matching issuedBy correctly" in {
      val country = Country("NL", "Netherlands")
      val etmpDetails = Seq(
        EtmpPreviousEuRegistrationDetails(
          issuedBy = etmpEuPreviousRegistrationDetails.issuedBy,
          registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber,
          schemeType = etmpEuPreviousRegistrationDetails.schemeType,
          intermediaryNumber = etmpEuPreviousRegistrationDetails.intermediaryNumber
        )
      )

      val expectedPreviousRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = country,
        previousSchemesDetails = Seq.empty
      )

      val result = PreviousRegistrationDetails.fromEtmpPreviousEuRegistrationDetails(country, etmpDetails)
      result mustBe expectedPreviousRegistrationDetails
    }

    "fail deserialization when previousSchemesDetails is not an array" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "FR",
           |    "name": "France"
           |  },
           |  "previousSchemesDetails": {}
           |}
           |""".stripMargin
      )

      intercept[JsResultException] {
        json.as[PreviousRegistrationDetails]
      }
    }
  }
}
