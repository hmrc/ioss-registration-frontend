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

import models.Country
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*


class OssPreviousRegistrationSpec extends AnyWordSpec with Matchers {

  "PreviousRegistration" must {

    "serialize and deserialize PreviousRegistrationNew correctly" in {
      val previousRegistrationNew = OssPreviousRegistrationNew(
        country = Country("AT", "Austria"),
        previousSchemesDetails = Seq(
          OssPreviousSchemeDetails(
            previousScheme = OssPreviousScheme.OSSU,
            previousSchemeNumbers = OssPreviousSchemeNumbers("12345", Some("98765"))
          )
        )
      )

      val json = Json.obj(
        "country" -> Json.obj (
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345",
              "previousIntermediaryNumber" -> "98765"
            )
          )
        )
      )

      Json.toJson(previousRegistrationNew) mustEqual json

      json.as[OssPreviousRegistration] mustEqual previousRegistrationNew
    }

    "serialize and deserialize PreviousRegistrationLegacy correctly" in {
      val previousRegistrationLegacy = OssPreviousRegistrationLegacy(
        country = Country("AT", "Austria"),
        vatNumber = "ATU123456789"
      )

      val json = Json.obj(
        "country" -> Json.obj (
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> "ATU123456789"
      )

      Json.toJson(previousRegistrationLegacy) mustEqual json

      json.as[OssPreviousRegistration] mustEqual previousRegistrationLegacy
    }

    "serialize and deserialize PreviousRegistration correctly (polymorphic)" in {
      val previousRegistrationNew = OssPreviousRegistrationNew(
        country = Country("AT", "Austria"),
        previousSchemesDetails = Seq(
          OssPreviousSchemeDetails(
            previousScheme = OssPreviousScheme.OSSU,
            previousSchemeNumbers = OssPreviousSchemeNumbers("12345", Some("98765"))
          )
        )
      )

      val jsonNew = Json.obj(
        "country" -> Json.obj (
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345",
              "previousIntermediaryNumber" -> "98765"
            )
          )
        )
      )

      Json.toJson(previousRegistrationNew) mustEqual jsonNew

      jsonNew.as[OssPreviousRegistration] mustEqual previousRegistrationNew

      val previousRegistrationLegacy = OssPreviousRegistrationLegacy(
        country = Country("AT", "Austria"),
        vatNumber = "ATU123456789"
      )

      val jsonLegacy = Json.obj(
        "country" -> Json.obj (
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> "ATU123456789"
      )


      Json.toJson(previousRegistrationLegacy) mustEqual jsonLegacy

      jsonLegacy.as[OssPreviousRegistration] mustEqual previousRegistrationLegacy
    }

  }

  "PreviousRegistrationNew" must {

    "must deserialise/serialise to and from PreviousRegistrationNew" in {
      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345",
              "previousIntermediaryNumber" -> "98765"
            )
          )
        )
      )

      val expectedResult = OssPreviousRegistrationNew(
        country = Country("DE", "Germany"),
        previousSchemesDetails = Seq(
          OssPreviousSchemeDetails(
            previousScheme = OssPreviousScheme.OSSU,
            previousSchemeNumbers = OssPreviousSchemeNumbers("12345", Some("98765"))
          )
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousRegistrationNew] mustBe JsSuccess(expectedResult)
    }

    "must handle optional fields when deserialise/serialise" in {
      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345"
            )
          )
        )
      )

      val expectedResult = OssPreviousRegistrationNew(
        country = Country("DE", "Germany"),
        previousSchemesDetails = Seq(
          OssPreviousSchemeDetails(
            previousScheme = OssPreviousScheme.OSSU,
            previousSchemeNumbers = OssPreviousSchemeNumbers("12345", None)
          )
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousRegistrationNew] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssPreviousRegistrationNew] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> 12345,
          "name" -> "Germany"
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345",
              "previousIntermediaryNumber" -> "98765"
            )
          )
        )
      )

      json.validate[OssPreviousRegistrationNew] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> JsNull
        ),
        "previousSchemesDetails" -> Json.arr(
          Json.obj(
            "previousScheme" -> "ossu",
            "previousSchemeNumbers" -> Json.obj(
              "previousSchemeNumber" -> "12345",
              "previousIntermediaryNumber" -> "98765"
            )
          )
        )
      )

      json.validate[OssPreviousRegistrationNew] mustBe a[JsError]
    }
  }

  "PreviousRegistrationLegacy" must {

    "must deserialise/serialise to and from PreviousRegistrationLegacy" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> "ATU123456789"
      )

      val expectedResult = OssPreviousRegistrationLegacy(
        country = Country("AT", "Austria"),
        vatNumber = "ATU123456789"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousRegistrationLegacy] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssPreviousRegistrationLegacy] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> 12345,
          "name" -> "Austria"
        ),
        "vatNumber" -> "ATU123456789"
      )

      json.validate[OssPreviousRegistrationLegacy] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> JsNull
        ),
        "vatNumber" -> "ATU123456789"
      )

      json.validate[OssPreviousRegistrationLegacy] mustBe a[JsError]
    }
  }

  "PreviousSchemeDetails" must {

    "must deserialise/serialise to and from TradeDetails" in {

      val json = Json.obj(
        "previousScheme" -> "ossu",
        "previousSchemeNumbers" -> Json.obj(
          "previousSchemeNumber" -> "12345",
          "previousIntermediaryNumber" -> "98765"
        )
      )

      val expectedResult = OssPreviousSchemeDetails(
        previousScheme = OssPreviousScheme.OSSU,
        previousSchemeNumbers = OssPreviousSchemeNumbers("12345", Some("98765"))
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousSchemeDetails] mustBe JsSuccess(expectedResult)
    }

    "must handle optional values when deserialise/serialise to and from TradeDetails" in {

      val json = Json.obj(
        "previousScheme" -> "ossu",
        "previousSchemeNumbers" -> Json.obj(
          "previousSchemeNumber" -> "12345"
        )
      )

      val expectedResult = OssPreviousSchemeDetails(
        previousScheme = OssPreviousScheme.OSSU,
        previousSchemeNumbers = OssPreviousSchemeNumbers("12345", None)
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousSchemeDetails] mustBe JsSuccess(expectedResult)
    }


    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssPreviousSchemeDetails] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "previousScheme" -> "ossu",
        "previousSchemeNumbers" -> Json.obj(
          "previousSchemeNumber" -> 12345,
          "previousIntermediaryNumber" -> "98765"
        )
      )

      json.validate[OssPreviousSchemeDetails] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "previousScheme" -> "ossu",
        "previousSchemeNumbers" -> Json.obj(
          "previousSchemeNumber" -> JsNull,
          "previousIntermediaryNumber" -> "98765"
        )
      )

      json.validate[OssPreviousSchemeDetails] mustBe a[JsError]
    }
  }

  "PreviousSchemeNumbers" must {

    "must deserialise/serialise to and from TradeDetails" in {

      val json = Json.obj(
        "previousSchemeNumber" -> "12345",
        "previousIntermediaryNumber" -> "98765"
      )

      val expectedResult = OssPreviousSchemeNumbers(
        previousSchemeNumber = "12345",
        previousIntermediaryNumber = Some("98765")
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousSchemeNumbers] mustBe JsSuccess(expectedResult)
    }

    "must handle optional values when deserialise/serialise to and from TradeDetails" in {

      val json = Json.obj(
        "previousSchemeNumber" -> "12345",
      )

      val expectedResult = OssPreviousSchemeNumbers(
        previousSchemeNumber = "12345",
        previousIntermediaryNumber = None
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssPreviousSchemeNumbers] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssPreviousSchemeNumbers] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "previousSchemeNumber" -> 12345,
        "previousIntermediaryNumber" -> "98765"
      )

      json.validate[OssPreviousSchemeNumbers] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "previousSchemeNumber" -> JsNull,
        "previousIntermediaryNumber" -> "98765"
      )

      json.validate[OssPreviousSchemeNumbers] mustBe a[JsError]
    }
  }
}