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

package models.etmp

import formats.Format.eisDateFormatter
import logging.Logging
import models.{BusinessContactDetails, UserAnswers}
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import play.api.libs.json.{Json, OFormat}
import queries.AllWebsites
import queries.tradingNames.AllTradingNames
import services.etmp.{EtmpEuRegistrations, EtmpPreviousEuRegistrations}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDateTime

final case class EtmpRegistrationRequest(
                                          administration: EtmpAdministration,
                                          customerIdentification: EtmpCustomerIdentification,
                                          tradingNames: Seq[EtmpTradingName],
                                          schemeDetails: EtmpSchemeDetails,
                                          bankDetails: EtmpBankDetails
                                        )

object EtmpRegistrationRequest extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {

  implicit val format: OFormat[EtmpRegistrationRequest] = Json.format[EtmpRegistrationRequest]

  // TODO Test no answers, full answers(where optianal values used 2 entries (countries) one with full, one with opt))
  def buildEtmpRegistrationRequest(answers: UserAnswers, vrn: Vrn, commencementDate: LocalDateTime): EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionCreate),
    customerIdentification = EtmpCustomerIdentification(vrn),
    tradingNames = getTradingNames(answers),
    schemeDetails = getSchemeDetails(answers, commencementDate),
    bankDetails = getBankDetails(answers)
  )

  private def getSchemeDetails(answers: UserAnswers, commencementDate: LocalDateTime): EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = commencementDate.format(eisDateFormatter),
    euRegistrationDetails = getEuTaxRegistrations(answers),
    previousEURegistrationDetails = getPreviousRegistrationDetails(answers),
    websites = getWebsites(answers),
    contactName = getBusinessContactDetails(answers).fullName,
    businessTelephoneNumber = getBusinessContactDetails(answers).telephoneNumber,
    businessEmailId = getBusinessContactDetails(answers).emailAddress,
    nonCompliantReturns = None, // TODO -> VEIOSS-256
    nonCompliantPayments = None // TODO -> VEIOSS-256
  )

  private def getTradingNames(answers: UserAnswers): List[EtmpTradingName] = {
    answers.get(HasTradingNamePage) match {
      case Some(true) =>
        answers.get(AllTradingNames) match {
          case Some(tradingNames) =>
            for {
              tradingName <- tradingNames
            } yield EtmpTradingName(tradingName = tradingName.name)
          case Some(Nil) | None =>
            val exception = new IllegalStateException("Must have trading names if HasTradingNamePage is Yes") // TODO
            logger.error(exception.getMessage, exception)
            throw exception
        }

      case Some(false) =>
        List.empty

      case None =>
        val exception = new IllegalStateException("Must select if trading name is different") // TODO
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getWebsites(answers: UserAnswers): List[EtmpWebsite] =
    answers.get(AllWebsites) match {
      case Some(websites) =>
        for {
          website <- websites
        } yield EtmpWebsite(websiteAddress = website.site)
      case _ =>
        val exception = new IllegalStateException("User must have at least one website")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def getBusinessContactDetails(answers: UserAnswers): BusinessContactDetails = {
    answers.get(BusinessContactDetailsPage) match {
      case Some(contactDetails) => contactDetails
      case _ =>
        val exception = new IllegalStateException("User must submit contact details")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getBankDetails(answers: UserAnswers): EtmpBankDetails =
    answers.get(BankDetailsPage) match {
      case Some(bankDetails) =>
        EtmpBankDetails(bankDetails.accountName, bankDetails.bic, bankDetails.iban)
      case _ =>
        val exception = new IllegalStateException("User must supply bank details")
        logger.error(exception.getMessage, exception)
        throw exception
    }
}
