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

import generators.Generators
import models.ossRegistration.OssEuTaxIdentifierType.Vat
import models.{Country, InternationalAddress}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class OssEuTaxRegistrationSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators {

  "EU Tax Registration" - {

    "must serialise and deserialise from / to an EU VAT Registration" in {

      val euVatNumberGen = arbitrary[Int].map(_.toString)

      forAll(arbitrary[Country], euVatNumberGen) {
        case (country, vatNumber) =>

          val euVatRegistration = OssEuVatRegistration(country, vatNumber)

          val json = Json.toJson(euVatRegistration)
          json.validate[OssEuTaxRegistration] mustEqual JsSuccess(euVatRegistration)
      }
    }

    "must serialise and deserialise from / to a Registration with Fixed Establishment" in {

      forAll(arbitrary[Country], arbitrary[OssTradeDetails], arbitrary[OssEuTaxIdentifier]) {
        case (country, fixedEstablishment, taxRef) =>

          val euRegistration = OssRegistrationWithFixedEstablishment(country, taxRef, fixedEstablishment)

          val json = Json.toJson(euRegistration)
          json.validate[OssEuTaxRegistration] mustEqual JsSuccess(euRegistration)
      }
    }

    "must serialise and deserialise from / to a Registration without Fixed Establishment" in {

      forAll(arbitrary[Country]) {
        country =>
          val euRegistration = OssRegistrationWithoutTaxId(country)

          val json = Json.toJson(euRegistration)
          json.validate[OssEuTaxRegistration] mustEqual JsSuccess(euRegistration)
      }
    }
  }

  "EuVatRegistration" - {

    "must deserialise/serialise to and from EuVatRegistration" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> "ATU123456789"
      )

      val expectedResult = OssEuVatRegistration(
        country = Country("AT", "Austria"),
        vatNumber = "ATU123456789"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssEuVatRegistration] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssEuVatRegistration] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> 12345
      )

      json.validate[OssEuVatRegistration] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "vatNumber" -> JsNull
      )

      json.validate[OssEuVatRegistration] mustBe a[JsError]
    }
  }

  "RegistrationWithFixedEstablishment" - {

    "must deserialise/serialise to and from RegistrationWithFixedEstablishment" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "fixedEstablishment" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> "DE",
              "name" -> "Germany"
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      val expectedResult = OssRegistrationWithFixedEstablishment(
        country = Country("AT", "Austria"),
        taxIdentifier = OssEuTaxIdentifier(
          identifierType = Vat,
          value = "123456789"
        ),
        fixedEstablishment = OssTradeDetails(
          tradingName = "Trading Name",
          address = InternationalAddress(
            line1 = "Line 1",
            line2 = Some("Line 2"),
            townOrCity = "Town",
            stateOrRegion = Some("Region"),
            postCode = Some("Postcode"),
            country = Country("DE", "Germany")
          )
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssRegistrationWithFixedEstablishment] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssRegistrationWithFixedEstablishment] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "fixedEstablishment" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> 12345,
              "name" -> "Germany"
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      json.validate[OssRegistrationWithFixedEstablishment] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "fixedEstablishment" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> "DE",
              "name" -> JsNull
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      json.validate[OssRegistrationWithFixedEstablishment] mustBe a[JsError]
    }
  }

  "RegistrationWithoutFixedEstablishmentWithTradeDetails" - {

    "must deserialise/serialise to and from RegistrationWithoutFixedEstablishmentWithTradeDetails" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "tradeDetails" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> "DE",
              "name" -> "Germany"
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      val expectedResult = OssRegistrationWithoutFixedEstablishmentWithTradeDetails(
        country = Country("AT", "Austria"),
        taxIdentifier = OssEuTaxIdentifier(
          identifierType = Vat,
          value = "123456789"

        ),
        tradeDetails = OssTradeDetails(
          tradingName = "Trading Name",
          address = InternationalAddress(
            line1 = "Line 1",
            line2 = Some("Line 2"),
            townOrCity = "Town",
            stateOrRegion = Some("Region"),
            postCode = Some("Postcode"),
            country = Country("DE", "Germany")
          )
        )

      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssRegistrationWithoutFixedEstablishmentWithTradeDetails] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssRegistrationWithoutFixedEstablishmentWithTradeDetails] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "tradeDetails" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> 12345,
              "name" -> "Germany"
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      json.validate[OssRegistrationWithoutFixedEstablishmentWithTradeDetails] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        ),
        "tradeDetails" -> Json.obj(
          "tradingName" -> "Trading Name",
          "address" -> Json.obj(
            "postCode" -> "Postcode",
            "country" -> Json.obj(
              "code" -> "DE",
              "name" -> JsNull
            ),
            "line1" -> "Line 1",
            "townOrCity" -> "Town",
            "line2" -> "Line 2",
            "stateOrRegion" -> "Region"
          )
        )
      )

      json.validate[OssRegistrationWithoutFixedEstablishmentWithTradeDetails] mustBe a[JsError]
    }
  }

  "RegistrationWithoutFixedEstablishment" - {

    "must deserialise/serialise to and from RegistrationWithoutFixedEstablishment" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        )
      )

      val expectedResult = OssRegistrationWithoutFixedEstablishment(
        country = Country("AT", "Austria"),
        taxIdentifier = OssEuTaxIdentifier(
          identifierType = Vat,
          value = "123456789"
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssRegistrationWithoutFixedEstablishment] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssRegistrationWithoutFixedEstablishment] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> 12345,
          "name" -> "Austria"
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        )
      )

      json.validate[OssRegistrationWithoutFixedEstablishment] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> JsNull
        ),
        "taxIdentifier" -> Json.obj(
          "identifierType" -> "vat",
          "value" -> "123456789"
        )
      )

      json.validate[OssRegistrationWithoutFixedEstablishment] mustBe a[JsError]
    }
  }

  "RegistrationWithoutTaxId" - {

    "must deserialise/serialise to and from RegistrationWithoutTaxId" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        )
      )

      val expectedResult = OssRegistrationWithoutTaxId(
        country = Country("AT", "Austria")
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[OssRegistrationWithoutTaxId] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[OssRegistrationWithoutTaxId] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> 12345
        )
      )

      json.validate[OssRegistrationWithoutTaxId] mustBe a[JsError]
    }

    "must handle null fields during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> JsNull,
          "name" -> "Austria"
        )
      )

      json.validate[OssRegistrationWithoutTaxId] mustBe a[JsError]
    }
  }
}
