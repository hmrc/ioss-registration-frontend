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

package models.domain

import models.Country
import models.PreviousScheme.{IOSSWI, IOSSWOI, OSSNU, OSSU, toEmtpSchemaType}
import models.etmp.{EtmpPreviousEuRegistrationDetails, SchemeType}
import models.previousRegistrations.NonCompliantDetails
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, Json}

class PreviousRegistrationSpec extends AnyFreeSpec with Matchers {

  private val allValues: Seq[EtmpPreviousEuRegistrationDetails] = List("CY", "DE", "IT", "DE", "DE")
    .zipWithIndex.map { case (index, countryCode) =>
      generateDetails(index, countryCode)
    }

  val germanCountry: Country = Country.fromCountryCodeUnsafe("DE")

  private val remappedGermanResults = PreviousRegistration(germanCountry, List(
    PreviousSchemeDetails(OSSNU, PreviousSchemeNumbers("DE-reg-1", None), None),
    PreviousSchemeDetails(IOSSWI, PreviousSchemeNumbers("DE-reg-3", None), None),
    PreviousSchemeDetails(OSSU, PreviousSchemeNumbers("DEDE-reg-4", None), None)
  ))

  "fromEtmpPreviousEuRegistrationDetailsByCountry group results by expected country" in {
    PreviousRegistration.fromEtmpPreviousEuRegistrationDetailsByCountry(
      country = germanCountry,
      etmpPreviousEuRegistrationDetails = allValues
    ) mustBe remappedGermanResults
  }

  private def generateDetails(countryCode: String, index: Int) = {
    val schemeType = SchemeType.values.toList(index % 4)
    EtmpPreviousEuRegistrationDetails(countryCode, s"$countryCode-reg-$index", schemeType, None)
  }

  "fromEtmpPreviousEuRegistrationDetails should convert" in {
    PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(
      allValues
    ) mustBe
      List(
        PreviousRegistration(
          Country.fromCountryCodeUnsafe("CY"),
          List(PreviousSchemeDetails(OSSU, PreviousSchemeNumbers("CYCY-reg-0", None), None))),
        PreviousRegistration(
          Country.fromCountryCodeUnsafe("IT"),
          List(PreviousSchemeDetails(IOSSWOI, PreviousSchemeNumbers("IT-reg-2", None), None))),
        remappedGermanResults
      )

  }

  "fromEtmpPreviousEuRegistrationDetailsByCountry handles empty and non-matching inputs" in {
    val emptyDetails = Seq.empty[EtmpPreviousEuRegistrationDetails]
    val nonMatchingDetails = Seq(
      EtmpPreviousEuRegistrationDetails("FR", "FR-reg-1", toEmtpSchemaType(OSSU), None)
    )

    PreviousRegistration.fromEtmpPreviousEuRegistrationDetailsByCountry(germanCountry, emptyDetails) mustBe
      PreviousRegistration(germanCountry, Seq.empty)

    PreviousRegistration.fromEtmpPreviousEuRegistrationDetailsByCountry(germanCountry, nonMatchingDetails) mustBe
      PreviousRegistration(germanCountry, Seq.empty)
  }

  "fromEtmpPreviousEuRegistrationDetails groups entries correctly" in {
    val input = Seq(
      EtmpPreviousEuRegistrationDetails("DE", "DE-reg-1", toEmtpSchemaType(OSSU), None),
      EtmpPreviousEuRegistrationDetails("FR", "FR-reg-2", toEmtpSchemaType(OSSNU), None),
      EtmpPreviousEuRegistrationDetails("DE", "DE-reg-3", toEmtpSchemaType(IOSSWI), None)
    )

    val result = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(input)

    result must contain theSameElementsAs List(
      PreviousRegistration(
        Country.fromCountryCodeUnsafe("DE"),
        List(
          PreviousSchemeDetails(OSSU, PreviousSchemeNumbers("DEDE-reg-1", None), None),
          PreviousSchemeDetails(IOSSWI, PreviousSchemeNumbers("DE-reg-3", None), None)
        )
      ),
      PreviousRegistration(
        Country.fromCountryCodeUnsafe("FR"),
        List(PreviousSchemeDetails(OSSNU, PreviousSchemeNumbers("FR-reg-2", None), None))
      )
    )
  }

  "JSON serialization and deserialization should work for PreviousRegistration" in {
    val registration = PreviousRegistration(
      Country.fromCountryCodeUnsafe("DE"),
      List(
        PreviousSchemeDetails(
          OSSU,
          PreviousSchemeNumbers("DEDE-reg-1", Some("INT-123")),
          Some(NonCompliantDetails(Some(1), Some(1)))
        )
      )
    )

    val json = Json.toJson(registration)
    Json.fromJson[PreviousRegistration](json).get mustBe registration
  }

  "must handle missing fields during deserialization" in {
    val json = Json.obj()

    json.validate[PreviousRegistration] mustBe a[JsError]
  }

  "must handle invalid data during deserialization" in {

    val json = Json.obj(
      "country" -> Json.obj(
        "code" -> 12345,
        "name" ->"Germany"
      ),
      "previousSchemesDetails" -> Json.obj(
        "previousScheme" -> "ossu",
        "previousSchemeNumbers" -> Json.obj(
          "previousSchemeNumber" -> "DEDE-reg-1",
          "previousIntermediaryNumber" -> "INT-123"
        ),
      ),
      "nonCompliantDetails" -> Json.obj(
        "nonCompliantReturns" -> 1,
        "nonCompliantPayments" -> 1
      )
    )

    json.validate[PreviousRegistration] mustBe a[JsError]

  }
}
