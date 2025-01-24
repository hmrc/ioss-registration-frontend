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

package models.euDetails

import base.SpecBase
import models.{Country, InternationalAddress}
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class EuDetailsSpec extends SpecBase with Matchers {

  "EuDetails" - {

    "must serialize to JSON correctly" in {

      val euDetails = EuDetails(
        euCountry = Country("DE", "Germany"),
        hasFixedEstablishment = Some(true),
        registrationType = Some(RegistrationType.TaxId),
        euVatNumber = Some("DK12345678"),
        euTaxReference = Some("ID1"),
        fixedEstablishmentTradingName = Some("fixedEstablishmentTradingName"),
        fixedEstablishmentAddress = Some(InternationalAddress(
          line1 = "Line 1",
          line2 = Some("Line 2"),
          townOrCity = "Town",
          stateOrRegion = Some("Region"),
          postCode = Some("AB12 3CD"),
          country = Country("DE", "Germany") )
        )
      )

      val expectedJson = Json.obj(
        "hasFixedEstablishment" -> true,
        "euVatNumber" -> "DK12345678",
        "fixedEstablishmentAddress" -> Json.obj(
          "line1" -> "Line 1",
          "townOrCity" -> "Town",
          "country" -> Json.obj(
            "code" -> "DE",
            "name" ->"Germany"
          ),
          "line2" -> "Line 2",
          "stateOrRegion" -> "Region",
          "postCode" -> "AB12 3CD"
        ),
        "euTaxReference" -> "ID1",
        "registrationType" -> "taxId",
        "euCountry" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "fixedEstablishmentTradingName" -> "fixedEstablishmentTradingName"
      )

      Json.toJson(euDetails) mustBe expectedJson

    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "hasFixedEstablishment" -> true,
        "euVatNumber" -> "DK12345678",
        "fixedEstablishmentAddress" -> Json.obj(
          "line1" -> "Line 1",
          "townOrCity" -> "Town",
          "country" -> Json.obj(
            "code" -> "DE",
            "name" -> "Germany"
          ),
          "line2" -> "Line 2",
          "stateOrRegion" -> "Region",
          "postCode" -> "AB12 3CD"
        ),
        "euTaxReference" -> "ID1",
        "registrationType" -> "taxId",
        "euCountry" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "fixedEstablishmentTradingName" -> "fixedEstablishmentTradingName"
      )

      val expectedEuDetails = EuDetails(
        euCountry = Country("DE", "Germany"),
        hasFixedEstablishment = Some(true),
        registrationType = Some(RegistrationType.TaxId),
        euVatNumber = Some("DK12345678"),
        euTaxReference = Some("ID1"),
        fixedEstablishmentTradingName = Some("fixedEstablishmentTradingName"),
        fixedEstablishmentAddress = Some(InternationalAddress(
          line1 = "Line 1",
          line2 = Some("Line 2"),
          townOrCity = "Town",
          stateOrRegion = Some("Region"),
          postCode = Some("AB12 3CD"),
          country = Country("DE", "Germany"))
        )
      )

      json.validate[EuDetails] mustBe JsSuccess(expectedEuDetails)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EuDetails] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "hasFixedEstablishment" -> 12345,
        "euVatNumber" -> "DK12345678",
        "fixedEstablishmentAddress" -> Json.obj(
          "line1" -> "Line 1",
          "townOrCity" -> "Town",
          "country" -> Json.obj(
            "code" -> "DE",
            "name" -> "Germany"
          ),
          "line2" -> "Line 2",
          "stateOrRegion" -> "Region",
          "postCode" -> "AB12 3CD"
        ),
        "euTaxReference" -> "ID1",
        "registrationType" -> "taxId",
        "euCountry" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "fixedEstablishmentTradingName" -> "fixedEstablishmentTradingName"
      )

      json.validate[EuDetails] mustBe a[JsError]
    }
  }
}
