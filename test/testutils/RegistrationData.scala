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

import base.SpecBase
import config.Constants.maxTradingNames
import formats.Format.eisDateFormatter
import models.etmp._
import models.{Bic, Country, Iban}
import models.etmp.amend.{EtmpAmendCustomerIdentification, EtmpAmendRegistrationChangeLog, EtmpAmendRegistrationRequest}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpDisplayEuRegistrationDetails: EtmpDisplayEuRegistrationDetails = EtmpDisplayEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    vatNumber = Some(Gen.alphaNumStr.sample.value),
    taxIdentificationNumber = None,
    fixedEstablishmentTradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(eisDateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Seq(arbitrary[EtmpWebsite].sample.value),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantReturns.toString),
    nonCompliantPayments = Some(arbitraryNonCompliantDetails.arbitrary.sample.value.nonCompliantPayments.toString)
  )

  val etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails = EtmpDisplaySchemeDetails(
    commencementDate = etmpSchemeDetails.commencementDate,
    euRegistrationDetails = Seq(etmpDisplayEuRegistrationDetails),
    previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails,
    websites = etmpSchemeDetails.websites,
    contactName = etmpSchemeDetails.contactName,
    businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
    businessEmailId = etmpSchemeDetails.businessEmailId,
    unusableStatus = false,
    nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns,
    nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments
  )

  val genBankDetails: EtmpBankDetails = EtmpBankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )

  val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails
  )

  val etmpAdminUse: EtmpAdminUse = EtmpAdminUse(
    changeDate = Some(LocalDateTime.now())
  )

  val etmpDisplayRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    schemeDetails = etmpDisplaySchemeDetails,
    bankDetails = genBankDetails,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    adminUse = etmpAdminUse
  )

  val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = EtmpAmendRegistrationRequest(
    administration = etmpRegistrationRequest.administration.copy(messageType = EtmpMessageType.IOSSSubscriptionAmend),
    changeLog = EtmpAmendRegistrationChangeLog(
      tradingNames = true,
      fixedEstablishments = true,
      contactDetails = true,
      bankDetails = true,
      reRegistration = false
    ),
    customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
    tradingNames = etmpRegistrationRequest.tradingNames,
    schemeDetails = etmpRegistrationRequest.schemeDetails,
    bankDetails = etmpRegistrationRequest.bankDetails
  )
}

