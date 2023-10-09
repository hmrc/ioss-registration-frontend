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

import generators.Generators
import models.InternationalAddress
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._

import java.time.LocalDateTime

class RegistrationSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with OptionValues {

  "Registration" - {

    "must exclude leading and trailing whitespace, and double spaces" in {

      val registeredCompanyName = "    registered company     name     "
      val expectedRegisteredCompanyName = "registered company name"

      val tradingNames = """[ "    trading      name     1    ", "    trading    name  2      " ]"""
      val expectedTradingNames = List("trading name 1", "trading name 2" )

      val businessContactName = "       Joe          Bloggs       "
      val expectedBusinessContactName = "Joe Bloggs"

      val vatDetailsAddressLine1 = "         123     Street    "
      val expectedVatDetailsAddressLine1 = "123 Street"

      val vatDetailsAddressLine2 = "         Street    "
      val expectedVatDetailsAddressLine2 = "Street"

      val vatDetailsAddressTownCity = "     City         "
      val expectedVatDetailsAddressTownCity = "City"

      val fixedEstablishmentAddressLine1 = "       Line       1       "
      val expectedFixedEstablishmentAddressLine1 = "Line 1"

      val fixedEstablishmentAddressTownCity = "     Town      "
      val expectedFixedEstablishmentAddressTownCity = "Town"

      val bankDetailsAccountName = "     Account name      "
      val expectedBankDetailsAccountName = "Account name"

      val json = s"""{
                   |  "vrn" : "123456789",
                   |  "registeredCompanyName" : "$registeredCompanyName",
                   |  "tradingNames" : $tradingNames,
                   |  "vatDetails" : {
                   |    "registrationDate" : "2022-04-21",
                   |    "address" : {
                   |      "line1" : "$vatDetailsAddressLine1",
                   |      "townOrCity" : "$vatDetailsAddressTownCity",
                   |      "postCode" : "AA12 1AB",
                   |      "country" : {
                   |        "code" : "GB",
                   |        "name" : "United Kingdom"
                   |      },
                   |      "line2" : "$vatDetailsAddressLine2",
                   |      "county" : "county"
                   |    },
                   |    "partOfVatGroup" : true,
                   |    "source" : "etmp"
                   |  },
                   |  "euRegistrations" : [ {
                   |    "country" : {
                   |      "code" : "FR",
                   |      "name" : "France"
                   |    },
                   |    "taxIdentifier" : {
                   |      "identifierType" : "vat",
                   |      "value" : "FR123456789"
                   |    }
                   |  }, {
                   |    "country" : {
                   |      "code" : "ES",
                   |      "name" : "Spain"
                   |    },
                   |    "taxIdentifier" : {
                   |      "identifierType" : "vat",
                   |      "value" : "ES123456789"
                   |    },
                   |    "fixedEstablishment" : {
                   |      "tradingName" : "Spanish trading name",
                   |      "address" : {
                   |        "line1" : "$fixedEstablishmentAddressLine1",
                   |        "townOrCity" : "$fixedEstablishmentAddressTownCity",
                   |        "country" : {
                   |          "code" : "ES",
                   |          "name" : "Spain"
                   |        }
                   |      }
                   |    }
                   |  }, {
                   |    "country" : {
                   |      "code" : "DE",
                   |      "name" : "Germany"
                   |    },
                   |    "taxIdentifier" : {
                   |      "identifierType" : "other",
                   |      "value" : "DE123456789"
                   |    },
                   |    "fixedEstablishment" : {
                   |      "tradingName" : "German trading name",
                   |      "address" : {
                   |        "line1" : "$fixedEstablishmentAddressLine1",
                   |        "townOrCity" : "$fixedEstablishmentAddressTownCity",
                   |        "country" : {
                   |          "code" : "DE",
                   |          "name" : "Germany"
                   |        }
                   |      }
                   |    }
                   |  }, {
                   |    "country" : {
                   |      "code" : "IE",
                   |      "name" : "Ireland"
                   |    },
                   |    "taxIdentifier" : {
                   |      "identifierType" : "other",
                   |      "value" : "IE123456789"
                   |    }
                   |  } ],
                   |  "contactDetails" : {
                   |    "fullName" : "$businessContactName",
                   |    "telephoneNumber" : "01112223344",
                   |    "emailAddress" : "email@email.com"
                   |  },
                   |  "websites" : [ "website1", "website2" ],
                   |  "commencementDate" : "2022-04-21",
                   |  "previousRegistrations" : [ {
                   |    "country" : {
                   |      "code" : "DE",
                   |      "name" : "Germany"
                   |    },
                   |    "previousSchemesDetails": [ {
                   |      "previousScheme": "ossu",
                   |      "previousSchemeNumbers": {
                   |        "previousSchemeNumber": "DE123"
                   |      }
                   |    } ]
                   |  } ],
                   |  "bankDetails" : {
                   |    "accountName" : "$bankDetailsAccountName",
                   |    "bic" : "ABCDGB2A",
                   |    "iban" : "GB33BUKB20201555555555"
                   |  },
                   |  "isOnlineMarketplace" : false,
                   |  "niPresence" : "principalPlaceOfBusinessInNi",
                   |  "dateOfFirstSale" : "2022-04-21",
                   |  "adminUse": {
                   |    "changeDate" : "${LocalDateTime.now()}"
                   |  }
                   |}""".stripMargin

      val registration = Json.parse(json).as[Registration]

      registration.registeredCompanyName mustEqual expectedRegisteredCompanyName
      registration.tradingNames mustEqual expectedTradingNames
      registration.contactDetails.fullName mustEqual expectedBusinessContactName
      registration.bankDetails.accountName mustEqual expectedBankDetailsAccountName

      val euTaxRegistrations = registration.euRegistrations
        .filter(_.isInstanceOf[RegistrationWithFixedEstablishment])
        .asInstanceOf[Seq[RegistrationWithFixedEstablishment]]

      val germanyRegistration = euTaxRegistrations.find(_.country.code == "DE").get
      germanyRegistration.fixedEstablishment.address.line1 mustEqual expectedFixedEstablishmentAddressLine1
      germanyRegistration.fixedEstablishment.address.townOrCity mustEqual expectedFixedEstablishmentAddressTownCity

      val spanishRegistration = euTaxRegistrations.find(_.country.code == "ES").get
      spanishRegistration.fixedEstablishment.address.line1 mustEqual expectedFixedEstablishmentAddressLine1
      spanishRegistration.fixedEstablishment.address.townOrCity mustEqual expectedFixedEstablishmentAddressTownCity

      registration.vatDetails.address.asInstanceOf[InternationalAddress].line1 mustEqual expectedVatDetailsAddressLine1
      registration.vatDetails.address.asInstanceOf[InternationalAddress].line2 mustEqual Some(expectedVatDetailsAddressLine2)
      registration.vatDetails.address.asInstanceOf[InternationalAddress].townOrCity mustEqual expectedVatDetailsAddressTownCity
    }
  }
}
