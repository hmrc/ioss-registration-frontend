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

package models.etmp.amend

import config.FrontendAppConfig
import models.UserAnswers
import models.etmp.*
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

case class EtmpAmendRegistrationRequest(
                                         administration: EtmpAdministration,
                                         changeLog: EtmpAmendRegistrationChangeLog,
                                         customerIdentification: EtmpAmendCustomerIdentification,
                                         tradingNames: Seq[EtmpTradingName],
                                         schemeDetails: EtmpSchemeDetails,
                                         bankDetails: EtmpBankDetails,
                                       )

object EtmpAmendRegistrationRequest {

  implicit val format: OFormat[EtmpAmendRegistrationRequest] = Json.format[EtmpAmendRegistrationRequest]

  def buildEtmpAmendRegistrationRequest(
                                         answers: UserAnswers,
                                         registration: EtmpDisplayRegistration,
                                         vrn: Vrn,
                                         iossNumber: String,
                                         commencementDate: LocalDate,
                                         rejoin: Boolean,
                                         appConfig: FrontendAppConfig
                                       ): EtmpAmendRegistrationRequest = {
    val etmpRegistrationRequest = buildEtmpRegistrationRequest(answers, vrn, commencementDate, appConfig)

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionAmend),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames =
          registration.tradingNames != etmpRegistrationRequest.tradingNames,
        fixedEstablishments =
          compareEuRegistrationDetails(
            registration.schemeDetails.euRegistrationDetails, etmpRegistrationRequest.schemeDetails.euRegistrationDetails,
          ),
        contactDetails =
          contactDetailsDiff(registration.schemeDetails, etmpRegistrationRequest.schemeDetails),
        bankDetails = registration.bankDetails != etmpRegistrationRequest.bankDetails,
        reRegistration = rejoin
      ),
      customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
      tradingNames = etmpRegistrationRequest.tradingNames,
      schemeDetails = etmpRegistrationRequest.schemeDetails,
      bankDetails = etmpRegistrationRequest.bankDetails
    )
  }

  private def contactDetailsDiff(
                                  registrationSchemeDetails: EtmpDisplaySchemeDetails,
                                  amendSchemeDetails: EtmpSchemeDetails
                                ): Boolean = {
    registrationSchemeDetails.contactName != amendSchemeDetails.contactName ||
      registrationSchemeDetails.businessTelephoneNumber != amendSchemeDetails.businessTelephoneNumber ||
      registrationSchemeDetails.businessEmailId != amendSchemeDetails.businessEmailId
  }

  private def compareEuRegistrationDetails(
                                            originalDetails: Seq[EtmpDisplayEuRegistrationDetails],
                                            amendedDetails: Seq[EtmpEuRegistrationDetails]
                                          ): Boolean = {
    val addedAdditionalCountries: Boolean = originalDetails.size != amendedDetails.size

    val hasOriginalDetailsChanged: Boolean = amendedDetails.zip(originalDetails).collect {
      case (amended, original) =>
        amended.countryOfRegistration != original.issuedBy ||
          compareTraderId(amended.traderId, original.vatNumber, original.taxIdentificationNumber) ||
          amended.tradingName != original.fixedEstablishmentTradingName ||
          amended.fixedEstablishmentAddressLine1 != original.fixedEstablishmentAddressLine1 ||
          amended.fixedEstablishmentAddressLine2 != original.fixedEstablishmentAddressLine2 ||
          amended.townOrCity != original.townOrCity ||
          amended.regionOrState != original.regionOrState ||
          amended.postcode != original.postcode
    }.contains(true)

    hasOriginalDetailsChanged || addedAdditionalCountries
  }

  private def compareTraderId(
                               traderId: TraderId,
                               maybeVatNumber: Option[String],
                               maybeEuTaxReference: Option[String]
                             ): Boolean = {
    traderId match {
      case vatNumberTraderId: VatNumberTraderId =>
        maybeVatNumber match {
          case Some(vatNumber) =>
            vatNumberTraderId.vatNumber != vatNumber

          case _ =>
            true
        }

      case taxRefTraderID: TaxRefTraderID =>
        maybeEuTaxReference match {
          case Some(euTaxReference) =>
            taxRefTraderID.taxReferenceNumber != euTaxReference

          case _ =>
            true
        }
    }
  }
}