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
import models.PreviousScheme.{IOSSWI, IOSSWOI, OSSNU, OSSU}
import models.etmp.{EtmpPreviousEuRegistrationDetails, SchemeType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

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
}
