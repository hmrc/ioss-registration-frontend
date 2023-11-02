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
import models.previousRegistrations.NonCompliantDetails
import models.{BusinessContactDetails, Index, UserAnswers}
import pages.tradingNames.HasTradingNamePage
import models.{BusinessContactDetails, UserAnswers}
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import pages.tradingNames.HasTradingNamePage
import play.api.libs.json.{Json, OFormat}
import queries.AllWebsites
import queries.previousRegistration.NonCompliantDetailsQuery
import queries.tradingNames.AllTradingNames
import services.etmp.{EtmpEuRegistrations, EtmpPreviousEuRegistrations}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

final case class EtmpRegistrationRequest(
                                          administration: EtmpAdministration,
                                          customerIdentification: EtmpCustomerIdentification,
                                          tradingNames: Seq[EtmpTradingName],
                                          schemeDetails: EtmpSchemeDetails,
                                          bankDetails: EtmpBankDetails
                                        )

object EtmpRegistrationRequest extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {

  implicit val format: OFormat[EtmpRegistrationRequest] = Json.format[EtmpRegistrationRequest]

  def buildEtmpRegistrationRequest(answers: UserAnswers, vrn: Vrn, commencementDate: LocalDate): EtmpRegistrationRequest =
    EtmpRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionCreate),
      customerIdentification = EtmpCustomerIdentification(vrn),
      tradingNames = getTradingNames(answers),
      schemeDetails = getSchemeDetails(answers, commencementDate),
      bankDetails = getBankDetails(answers)
    )

  private def getSchemeDetails(answers: UserAnswers, commencementDate: LocalDate): EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = commencementDate.format(eisDateFormatter),
    euRegistrationDetails = getEuTaxRegistrations(answers),
    previousEURegistrationDetails = getPreviousRegistrationDetails(answers),
    websites = getWebsites(answers),
    contactName = getBusinessContactDetails(answers).fullName,
    businessTelephoneNumber = getBusinessContactDetails(answers).telephoneNumber,
    businessEmailId = getBusinessContactDetails(answers).emailAddress,
    nonCompliantReturns = getNonCompliantDetails(answers).nonCompliantReturns,
    nonCompliantPayments = getNonCompliantDetails(answers).nonCompliantPayments
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
            val exception = new IllegalStateException("Must have at least one trading name")
            logger.error(exception.getMessage, exception)
            throw exception
        }

      case Some(false) =>
        List.empty

      case None =>
        val exception = new IllegalStateException("Must select Yes if trading name is different")
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
        val exception = new IllegalStateException("User must provide contact details")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getBankDetails(answers: UserAnswers): EtmpBankDetails =
    answers.get(BankDetailsPage) match {
      case Some(bankDetails) =>
        EtmpBankDetails(bankDetails.accountName, bankDetails.bic, bankDetails.iban)
      case _ =>
        val exception = new IllegalStateException("User must provide bank details")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def getNonCompliantDetails(answers: UserAnswers, index: Index): NonCompliantDetails =
    answers.get(NonCompliantDetailsQuery(index)) match {
      case Some(nonCompliantDetails) =>
        NonCompliantDetails(
          nonCompliantReturns = nonCompliantDetails.nonCompliantReturns,
          nonCompliantPayments = nonCompliantDetails.nonCompliantPayments
        )
    }
}
