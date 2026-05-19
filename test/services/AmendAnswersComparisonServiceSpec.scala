/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import base.SpecBase
import models.domain.PreviousSchemeDetails
import models.etmp.*
import models.euDetails.{EuDetails, EuOptionalDetails, RegistrationType}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{BankDetails, BusinessContactDetails, Country, InternationalAddress, TradingName, UserAnswers, Website}
import org.scalatest.BeforeAndAfterEach
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import queries.AllWebsites
import queries.euDetails.{AllEuDetailsQuery, AllEuOptionalDetailsQuery}
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import testutils.RegistrationData.{completeUserAnswersWithVatInfo, etmpDisplayRegistration}
import testutils.WireMockHelper

class AmendAnswersComparisonServiceSpec extends SpecBase with WireMockHelper with BeforeAndAfterEach {

  private val service = new AmendAnswersComparisonService()

  private val originalAnswers: EtmpDisplayRegistration =
    etmpDisplayRegistration

  ".answersHaveChanged" - {

    "return false when nothing has changed" in {

      val userAnswers = userAnswersMatching(originalAnswers)

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe false
    }

    "return true when trading names have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(AllTradingNames, List(TradingName("Changed trading name"))).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when previous registrations have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          AllPreviousRegistrationsQuery,
          List(
            PreviousRegistrationDetails(
              previousEuCountry = Country.fromCountryCodeUnsafe("FR"),
              previousSchemesDetails = Seq.empty
            )
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when previous registration schemes have changed" in {
      val existingPreviousRegistration =
        originalAnswers.schemeDetails.previousEURegistrationDetails.head

      val country =
        Country.fromCountryCodeUnsafe(existingPreviousRegistration.issuedBy)

      val changedPreviousSchemeDetails =
        PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(
          existingPreviousRegistration.copy(registrationNumber = "CHANGED123456")
        )

      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          AllPreviousRegistrationsQuery,
          List(
            PreviousRegistrationDetails(
              previousEuCountry = country,
              previousSchemesDetails = Seq(changedPreviousSchemeDetails)
            )
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }


    "return true when registered in EU have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          AllEuDetailsQuery,
          List(
            EuDetails(
              euCountry = Country.fromCountryCodeUnsafe("FR"),
              hasFixedEstablishment = Some(true),
              registrationType = None,
              euVatNumber = None,
              euTaxReference = None,
              fixedEstablishmentTradingName = None,
              fixedEstablishmentAddress = None
            )
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when EU details have changed" in {
      val existingEuDetails =
        originalAnswers.schemeDetails.euRegistrationDetails.head

      val changedEuOptionalDetails =
        toEuOptionalDetails(existingEuDetails).copy(
          fixedEstablishmentTradingName = Some("Changed trading name")
        )

      val remainingEuDetails =
        originalAnswers.schemeDetails.euRegistrationDetails.tail.map(toEuOptionalDetails).toList

      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          AllEuOptionalDetailsQuery,
          changedEuOptionalDetails :: remainingEuDetails
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when website details have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(AllWebsites, List(Website("https://changed.example.com"))).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when contact details have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          BusinessContactDetailsPage,
          BusinessContactDetails(
            fullName = "Changed Name",
            telephoneNumber = originalAnswers.schemeDetails.businessTelephoneNumber,
            emailAddress = originalAnswers.schemeDetails.businessEmailId
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when bank details have changed" in {
      val userAnswers = userAnswersMatching(originalAnswers)
        .set(
          BankDetailsPage,
          BankDetails(
            accountName = "Changed Account Name",
            bic = originalAnswers.bankDetails.bic,
            iban = originalAnswers.bankDetails.iban
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }
  }

  private def userAnswersMatching(original: EtmpDisplayRegistration): UserAnswers =
    completeUserAnswersWithVatInfo
      .set(AllTradingNames, original.tradingNames.map(x => TradingName(x.tradingName)).toList).success.value
      .set(AllPreviousRegistrationsQuery, original.schemeDetails.previousEURegistrationDetails
          .groupBy(_.issuedBy)
          .map { case (countryCode, registrations) =>
            PreviousRegistrationDetails.fromEtmpPreviousEuRegistrationDetails(
              Country.fromCountryCodeUnsafe(countryCode),
              registrations
            )
          }.toList
      ).success.value
      .set(AllEuDetailsQuery, original.schemeDetails.euRegistrationDetails.map { euDetails =>
          EuDetails(
            euCountry = Country.fromCountryCodeUnsafe(euDetails.issuedBy),
            hasFixedEstablishment = Some(true),
            registrationType = None,
            euVatNumber = None,
            euTaxReference = None,
            fixedEstablishmentTradingName = None,
            fixedEstablishmentAddress = None
          )
        }.toList
      ).success.value
      .set(AllEuOptionalDetailsQuery, original.schemeDetails.euRegistrationDetails.map(toEuOptionalDetails).toList).success.value
      .set(AllWebsites, original.schemeDetails.websites.map(x => Website(x.websiteAddress)).toList).success.value
      .set(BusinessContactDetailsPage, BusinessContactDetails(
          fullName = original.schemeDetails.contactName,
          telephoneNumber = original.schemeDetails.businessTelephoneNumber,
          emailAddress = original.schemeDetails.businessEmailId
        )
      ).success.value
      .set(BankDetailsPage, BankDetails(
          accountName = original.bankDetails.accountName,
          bic = original.bankDetails.bic,
          iban = original.bankDetails.iban
        )
      ).success.value

  private def toEuOptionalDetails(original: EtmpDisplayEuRegistrationDetails): EuOptionalDetails = {

    val country = Country.fromCountryCodeUnsafe(original.issuedBy)

    EuOptionalDetails(
      euCountry = country,
      fixedEstablishmentTradingName = Some(original.fixedEstablishmentTradingName),
      fixedEstablishmentAddress = Some(
        InternationalAddress(
          line1 = original.fixedEstablishmentAddressLine1,
          line2 = original.fixedEstablishmentAddressLine2,
          townOrCity = original.townOrCity,
          stateOrRegion = original.regionOrState,
          postCode = original.postcode,
          country = country
        )
      ),
      euVatNumber = original.vatNumber.map { vat =>
        if (vat.startsWith(original.issuedBy)) vat else s"${original.issuedBy}$vat"
      },
      euTaxReference = original.taxIdentificationNumber,
      hasFixedEstablishment = None,
      registrationType = None
    )
  }
}