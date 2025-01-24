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

import models.{Country, PreviousScheme}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._

class PreviousRegistrationDetailsWithOptionalVatNumberSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PreviousRegistrationDetailsWithOptionalVatNumber" - {

    "serialize to JSON correctly" in {
      val previousEuCountry = Country("FR", "France")
      val schemeNumbers = SchemeNumbersWithOptionalVatNumber(
        previousSchemeNumber = Some("12345"),
        previousIntermediaryNumber = Some("67890")
      )
      val schemeDetails = SchemeDetailsWithOptionalVatNumber(
        previousScheme = Some(PreviousScheme.OSSU),
        previousSchemeNumbers = Some(schemeNumbers)
      )
      val previousRegistrationDetails = PreviousRegistrationDetailsWithOptionalVatNumber(
        previousEuCountry = previousEuCountry,
        previousSchemesDetails = Some(List(schemeDetails))
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
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetailsWithOptionalVatNumber(
        previousEuCountry = Country("FR", "France"),
        previousSchemesDetails = Some(List(SchemeDetailsWithOptionalVatNumber(
          previousScheme = Some(PreviousScheme.OSSU),
          previousSchemeNumbers = Some(SchemeNumbersWithOptionalVatNumber(
            previousSchemeNumber = Some("12345"),
            previousIntermediaryNumber = Some("67890")
          ))
        )))
      )

      json.as[PreviousRegistrationDetailsWithOptionalVatNumber] mustBe expectedRegistrationDetails
    }

    "handle missing optional fields during deserialization" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousEuCountry": {
           |    "code": "FR",
           |    "name": "France"
           |  }
           |}
           |""".stripMargin
      )

      val expectedRegistrationDetails = PreviousRegistrationDetailsWithOptionalVatNumber(
        previousEuCountry = Country("FR", "France"),
        previousSchemesDetails = None
      )

      json.as[PreviousRegistrationDetailsWithOptionalVatNumber] mustBe expectedRegistrationDetails
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
        json.as[PreviousRegistrationDetailsWithOptionalVatNumber]
      }
    }
  }

  "SchemeDetailsWithOptionalVatNumber" - {

    "serialize to JSON correctly" in {
      val schemeNumbers = SchemeNumbersWithOptionalVatNumber(
        previousSchemeNumber = Some("12345"),
        previousIntermediaryNumber = Some("67890")
      )
      val schemeDetails = SchemeDetailsWithOptionalVatNumber(
        previousScheme = Some(PreviousScheme.OSSU),
        previousSchemeNumbers = Some(schemeNumbers)
      )

      val expectedJson: JsValue = Json.parse(
        s"""
           |{
           |  "previousScheme": "ossu",
           |  "previousSchemeNumbers": {
           |    "previousSchemeNumber": "12345",
           |    "previousIntermediaryNumber": "67890"
           |  }
           |}
           |""".stripMargin
      )

      Json.toJson(schemeDetails) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousScheme": "ossu",
           |  "previousSchemeNumbers": {
           |    "previousSchemeNumber": "12345",
           |    "previousIntermediaryNumber": "67890"
           |  }
           |}
           |""".stripMargin
      )

      val expectedSchemeDetails = SchemeDetailsWithOptionalVatNumber(
        previousScheme = Some(PreviousScheme.OSSU),
        previousSchemeNumbers = Some(SchemeNumbersWithOptionalVatNumber(
          previousSchemeNumber = Some("12345"),
          previousIntermediaryNumber = Some("67890")
        ))
      )

      json.as[SchemeDetailsWithOptionalVatNumber] mustBe expectedSchemeDetails
    }

    "handle missing optional fields during deserialization" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "previousScheme": null
           |}
           |""".stripMargin
      )

      val expectedSchemeDetails = SchemeDetailsWithOptionalVatNumber(
        previousScheme = None,
        previousSchemeNumbers = None
      )

      json.as[SchemeDetailsWithOptionalVatNumber] mustBe expectedSchemeDetails
    }
  }
}
