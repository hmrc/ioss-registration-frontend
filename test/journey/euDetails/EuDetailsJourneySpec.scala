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

package journey.euDetails

import generators.{Generators, ModelGenerators}
import journey.JourneyHelpers
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, Index}
import org.scalatest.freespec.AnyFreeSpec
import pages.CheckYourAnswersPage
import pages.euDetails._
import queries.euDetails.{AllEuDetailsRawQuery, EuDetailsQuery}

class EuDetailsJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators with Generators {

  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)

  private val tradingName = stringsWithMaxLength(40).sample.value
  private val tradingName2 = stringsWithMaxLength(40).sample.value
  private val euTaxReference = stringsWithMaxLength(20).sample.value

  private val initialise = journeyOf(
    setUserAnswerTo(TaxRegisteredInEuPage, true),
    setUserAnswerTo(EuCountryPage(countryIndex1), country),
    setUserAnswerTo(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
    setUserAnswerTo(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
    setUserAnswerTo(EuVatNumberPage(countryIndex1), euVatNumber),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex1)), true),
    setUserAnswerTo(EuCountryPage(countryIndex2), arbitraryCountry.arbitrary.sample.value),
    setUserAnswerTo(SellsGoodsToEuConsumerMethodPage(countryIndex2), EuConsumerSalesMethod.FixedEstablishment),
    setUserAnswerTo(RegistrationTypePage(countryIndex2), RegistrationType.TaxId),
    setUserAnswerTo(EuTaxReferencePage(countryIndex2), euTaxReference),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex2), tradingName),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex2), arbitraryInternationalAddress.arbitrary.sample.value),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex2)), false),
    goTo(CheckYourAnswersPage)
  )

  "users with one or more EU registration" - {

    "the user can't register a country as they sell their goods via a dispatch warehouse from that country" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.DispatchWarehouse),
          pageMustBe(CannotRegisterFixedEstablishmentOperationOnlyPage)
        )
    }

    "the user registers a country with a VAT number" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
          submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1))
        )
    }

    "the user registers a country with an EU Tax Reference" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
          submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex1), euTaxReference),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1))
        )
    }

    "must be able to remove them" - {

      "when there is only one" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex1), country),
            submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
            submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex1), euTaxReference),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goTo(DeleteEuDetailsPage(Index(0))),
            removeAddToListItem(EuDetailsQuery(Index(0))),
            pageMustBe(TaxRegisteredInEuPage),
            answersMustNotContain(EuDetailsQuery(Index(0)))
          )
      }

      "when there are multiple" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex1), country),
            submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
            submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
            submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goTo(AddEuDetailsPage(Some(countryIndex1))),
            submitAnswer(AddEuDetailsPage(Some(countryIndex1)), true),
            submitAnswer(EuCountryPage(countryIndex2), arbitraryCountry.arbitrary.sample.value),
            submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex2), EuConsumerSalesMethod.FixedEstablishment),
            submitAnswer(RegistrationTypePage(countryIndex2), RegistrationType.TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex2), euTaxReference),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), tradingName),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex2), arbitraryInternationalAddress.arbitrary.sample.value),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goTo(DeleteEuDetailsPage(countryIndex1)),
            removeAddToListItem(EuDetailsQuery(countryIndex1)),
            pageMustBe(AddEuDetailsPage()),
            answersMustNotContain(EuDetailsQuery(countryIndex2))
          )
      }
    }

    "must be able to change the users original answer" - {

      "when there is only one" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
          submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex1), euTaxReference),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
          goTo(AddEuDetailsPage(Some(countryIndex1))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex1)), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex1)),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goToChangeAnswer(RegistrationTypePage(countryIndex1)),
            pageMustBe(RegistrationTypePage(countryIndex1)),
            submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
            pageMustBe(EuVatNumberPage(countryIndex1)),
            submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goTo(AddEuDetailsPage(Some(countryIndex1))),
            answerMustEqual(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
            answerMustEqual(EuVatNumberPage(countryIndex1), euVatNumber),
            answersMustNotContain(EuTaxReferencePage(countryIndex1))
          )
      }

      "when there are multiple changes required" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex1), EuConsumerSalesMethod.FixedEstablishment),
          submitAnswer(RegistrationTypePage(countryIndex1), RegistrationType.VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), tradingName),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), arbitraryInternationalAddress.arbitrary.sample.value),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
          goTo(AddEuDetailsPage(Some(countryIndex1))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex1)), true),
          submitAnswer(EuCountryPage(countryIndex2), arbitraryCountry.arbitrary.sample.value),
          submitAnswer(SellsGoodsToEuConsumerMethodPage(countryIndex2), EuConsumerSalesMethod.FixedEstablishment),
          submitAnswer(RegistrationTypePage(countryIndex2), RegistrationType.TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex2), euTaxReference),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), tradingName),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex2), arbitraryInternationalAddress.arbitrary.sample.value),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
          goTo(AddEuDetailsPage(Some(countryIndex2))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex2)), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex2)),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goToChangeAnswer(FixedEstablishmentTradingNamePage(countryIndex2)),
            pageMustBe(FixedEstablishmentTradingNamePage(countryIndex2)),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), tradingName2),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goTo(AddEuDetailsPage(Some(countryIndex2))),
            answerMustEqual(FixedEstablishmentTradingNamePage(countryIndex2), tradingName2)
          )
      }
    }

    "must be able to remove all original EU registrations answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of being registered for tax in other EU countries to No" in {

        startingFrom(CheckYourAnswersPage)
          .run(
            initialise,
            goToChangeAnswer(TaxRegisteredInEuPage),
            submitAnswer(TaxRegisteredInEuPage, false),
            pageMustBe(DeleteAllEuDetailsPage),
            submitAnswer(DeleteAllEuDetailsPage, true),
            removeAddToListItem(AllEuDetailsRawQuery),
            pageMustBe(CheckYourAnswersPage),
            answersMustNotContain(EuCountryPage(countryIndex1)),
            answersMustNotContain(EuCountryPage(countryIndex2))
          )
      }
    }

    "must be able to retain all original EU registrations answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of being registered for tax in other EU countries to No" - {

        "but answer no when asked if they want to remove all EU registrations from the scheme" in {

          startingFrom(CheckYourAnswersPage)
            .run(
              initialise,
              goToChangeAnswer(TaxRegisteredInEuPage),
              submitAnswer(TaxRegisteredInEuPage, false),
              pageMustBe(DeleteAllEuDetailsPage),
              submitAnswer(DeleteAllEuDetailsPage, false),
              pageMustBe(CheckYourAnswersPage),
              answersMustContain(EuCountryPage(countryIndex1)),
              answersMustContain(EuCountryPage(countryIndex2))
            )
        }
      }
    }
  }
}
