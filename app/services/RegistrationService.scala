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

package services

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.RegistrationResultResponse
import formats.Format.eisDateFormatter
import logging.Logging
import models.etmp._
import models.{BusinessContactDetails, UserAnswers}
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import queries.AllWebsites
import queries.tradingNames.AllTradingNames
import services.etmp.{EtmpEuRegistrations, EtmpPreviousEuRegistrations}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.Future

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {

  def createRegistrationRequest(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, vrn))
  }

  private def buildEtmpRegistrationRequest(answers: UserAnswers, vrn: Vrn): EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionCreate),
    customerIdentification = EtmpCustomerIdentification(vrn),
    tradingNames = getTradingNames(answers),
    schemeDetails = getSchemeDetails(answers),
    bankDetails = getBankDetails(answers)
  )

  private def getSchemeDetails(answers: UserAnswers): EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDateTime.now(clock).format(eisDateFormatter),
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

        // TODO
      case Some(false) =>
        answers.get(AllTradingNames) match {
          case Some(_) => List.empty
          case Some(Nil) | None => List.empty
        }

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

