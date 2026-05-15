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


import models.domain.PreviousSchemeDetails
import models.etmp.*
import models.euDetails.{EuDetails, EuOptionalDetails}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{BankDetails, BusinessContactDetails, TradingName, UserAnswers, Website}
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import queries.AllWebsites
import queries.euDetails.{AllEuDetailsQuery, AllEuOptionalDetailsQuery}
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames

import javax.inject.Inject

class AmendAnswersComparisonService @Inject()() {

  def answersHaveChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    tradingNamesChanged(originalAnswers, userAnswers) ||
      previousRegistrationsChanged(originalAnswers, userAnswers) ||
      previousRegistrationSchemesChanged(originalAnswers, userAnswers) ||
      registeredInEuChanged(originalAnswers, userAnswers) ||
      euDetailsChanged(originalAnswers, userAnswers) ||
      websitesChanged(originalAnswers, userAnswers) ||
      contactDetailsChanged(originalAnswers, userAnswers) ||
      bankDetailsChanged(originalAnswers, userAnswers)
  }

  private def tradingNamesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllTradingNames).map(_.map(_.name)).getOrElse(Seq.empty) !=
      originalAnswers.tradingNames.map(_.tradingName)
  }

  private def previousRegistrationsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
  }

  private def previousRegistrationSchemesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(Seq.empty).exists { amendedCountry =>
      val matchingOriginalRegistrations =
        originalAnswers.schemeDetails.previousEURegistrationDetails
          .filter(_.issuedBy == amendedCountry.previousEuCountry.code)

      val originalSchemeNumbers =
        matchingOriginalRegistrations.map { registration =>
          PreviousSchemeDetails
            .fromEtmpPreviousEuRegistrationDetails(registration)
            .previousSchemeNumbers
        }

      val amendedSchemeNumbers =
        amendedCountry.previousSchemesDetails.map(_.previousSchemeNumbers)

      originalSchemeNumbers.nonEmpty && amendedSchemeNumbers != originalSchemeNumbers
    }
  }

  private def registeredInEuChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.euRegistrationDetails.map(_.issuedBy)
  }

  private def euDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllEuOptionalDetailsQuery).getOrElse(Seq.empty).exists { userDetail =>
      originalAnswers.schemeDetails.euRegistrationDetails
        .find(_.issuedBy == userDetail.euCountry.code)
        .exists(registrationDetail => hasDetailsChanged(userDetail, registrationDetail))
    }
  }

  private def websitesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllWebsites).map(_.map(_.site)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.websites.map(_.websiteAddress)
  }

  private def contactDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(BusinessContactDetailsPage).exists { contactDetails =>
      contactDetails.fullName != originalAnswers.schemeDetails.contactName ||
        contactDetails.telephoneNumber != originalAnswers.schemeDetails.businessTelephoneNumber ||
        contactDetails.emailAddress != originalAnswers.schemeDetails.businessEmailId
    }
  }

  private def bankDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(BankDetailsPage).exists { bankDetails =>
      bankDetails.accountName != originalAnswers.bankDetails.accountName ||
        bankDetails.bic != originalAnswers.bankDetails.bic ||
        bankDetails.iban != originalAnswers.bankDetails.iban
    }
  }

  private def hasDetailsChanged(userDetails: EuOptionalDetails, registrationDetails: EtmpDisplayEuRegistrationDetails): Boolean = {
    val vatNumberWithoutCountryCode =
      userDetails.euVatNumber.map(_.stripPrefix(userDetails.euCountry.code))

    val registrationVatNumber =
      registrationDetails.vatNumber

    userDetails.fixedEstablishmentTradingName.exists(_ != registrationDetails.fixedEstablishmentTradingName) ||
      userDetails.fixedEstablishmentAddress.exists { address =>
        address.line1 != registrationDetails.fixedEstablishmentAddressLine1 ||
          address.line2 != registrationDetails.fixedEstablishmentAddressLine2 ||
          address.townOrCity != registrationDetails.townOrCity ||
          address.stateOrRegion != registrationDetails.regionOrState ||
          address.postCode != registrationDetails.postcode
      } ||
      vatNumberWithoutCountryCode != registrationVatNumber ||
      userDetails.euTaxReference != registrationDetails.taxIdentificationNumber
  }
}

