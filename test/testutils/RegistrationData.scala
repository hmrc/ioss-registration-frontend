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

package testutils

import generators.Generators
import models.domain._
import models.{BankDetails, Bic, BusinessContactDetails, Country, DesAddress, Iban, InternationalAddress, PreviousScheme}
import org.scalatest.EitherValues
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

object RegistrationData extends Generators with EitherValues {

  val iban: Iban = Iban("GB33BUKB20201555555555").value
  val bic: Bic = Bic("ABCDGB2A").get

  val registration: Registration =
    Registration(
      vrn = Vrn("123456789"),
      registeredCompanyName = "foo",
      tradingNames = List("single", "double"),
      vatDetails = VatDetails(
        registrationDate = LocalDate.now,
        address          = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
        partOfVatGroup   = false,
        source           = VatDetailSource.Etmp
      ),
      euRegistrations = Seq(
        RegistrationWithoutFixedEstablishmentWithTradeDetails(Country("FR", "France"),
          EuTaxIdentifier(EuTaxIdentifierType.Vat, Some("FRAA123456789")),
          TradeDetails(
            "French trading name",
          InternationalAddress(
            line1 = "Line 1",
            line2 = None,
            townOrCity = "Town",
            stateOrRegion = None,
            None,
            Country("FR", "France")
          ))
        ),
        EuVatRegistration(
          Country("DE", "Germany"),
          vatNumber = "DE123456789"
        ),
        RegistrationWithoutFixedEstablishmentWithTradeDetails(
          Country("IE", "Ireland"),
          EuTaxIdentifier(EuTaxIdentifierType.Other, Some("IE1234567AB")),
          TradeDetails(
          "Irish trading name",
          InternationalAddress(
            line1 = "Line 1",
            line2 = None,
            townOrCity = "Town",
            stateOrRegion = None,
            None,
            Country("IE", "Ireland")
          ))
        ),
        RegistrationWithFixedEstablishment(
          Country("ES", "Spain"),
          EuTaxIdentifier(EuTaxIdentifierType.Vat, Some("ESA1234567A")),
          TradeDetails("Spanish trading name", InternationalAddress("Line 1", None, "Town", None, None, Country("ES", "Spain")))
        ),
        RegistrationWithFixedEstablishment(
          Country("DK", "Denmark"),
          EuTaxIdentifier(EuTaxIdentifierType.Other, Some("DK12345678")),
          TradeDetails("Danish trading name", InternationalAddress("Line 1", None, "Town", None, None, Country("DK", "Denmark")))
        ),
      ),
      contactDetails = createBusinessContactDetails(),
      websites = Seq("website1", "website2"),
      previousRegistrations = Seq(
        PreviousRegistrationNew(
          country = Country("DE", "Germany"),
          previousSchemesDetails = Seq(
            PreviousSchemeDetails(
              previousScheme = PreviousScheme.OSSU,
              previousSchemeNumbers = PreviousSchemeNumbers("DE123", None)
            )
          )
        )
      ),
      bankDetails = BankDetails("Account name", Some(bic), iban)
    )


  private def createBusinessContactDetails(): BusinessContactDetails =
    BusinessContactDetails(
      "Joe Bloggs",
      "01112223344",
      "email@email.com"
    )
}

