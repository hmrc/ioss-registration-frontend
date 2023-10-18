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

import formats.Format.eisDateTimeFormatter
import logging.Logging
import models.etmp._
import models.{BankDetails, BusinessContactDetails, UserAnswers, Website}
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import queries.AllWebsites
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.domain.Vrn

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject

case class RegistrationService @Inject()(clock: Clock) extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {


  def buildEtmpRegistrationRequest(answers: UserAnswers, vrn: Vrn): EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionCreate),
    customerIdentification = EtmpCustomerIdentification(vrn),
    tradingNames = getTradingNames(answers),
    schemeDetails = getSchemeDetails(answers),
    bankDetails = getBankDetails(answers)
  )

  private def getSchemeDetails(answers: UserAnswers): EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDateTime.now(clock).format(eisDateTimeFormatter),
    euRegistrationDetails = getEuTaxRegistrations(answers),
    previousEURegistrationDetails = getPreviousRegistrationDetails(answers),
    websites = getWebsites(answers),
    contactName = getBusinessContactDetails(answers).fullName,
    businessTelephoneNumber = getBusinessContactDetails(answers).telephoneNumber,
    businessEmailId = getBusinessContactDetails(answers).emailAddress,
    nonCompliantReturns = None, // TODO -> where/how are these populated
    nonCompliantPayments = None // TODO
  )

  private def getTradingNames(answers: UserAnswers): List[EtmpTradingName] = {
    answers.get(HasTradingNamePage) match {
      case Some(true) =>
        answers.get(AllTradingNames) match {
          case Some(tradingNames) =>
            for {
              tradingName <- tradingNames
            } yield EtmpTradingName(tradingName = tradingName.name)
          case _ => throw new Exception("No trading names found")
        }
      case Some(false) =>
        List.empty
      case _ =>
        logger.error("Has trading name was not answered")
        throw new Exception("Has Trading Name answer must not be empty")
    }
  }

  private def getWebsites(answers: UserAnswers): List[Website] =
    answers.get(AllWebsites) match {
      case Some(websites) => websites
      case _ =>
        logger.error("User has not submitted at least one website")
        throw new Exception("User must have at least one website")
    }

  private def getBusinessContactDetails(answers: UserAnswers): BusinessContactDetails = {
    answers.get(BusinessContactDetailsPage) match {
      case Some(contactDetails) => contactDetails
      case _ =>
        logger.error("User has not supplied any contact details")
        throw new Exception("User must submit contact details")
    }
  }

  private def getBankDetails(answers: UserAnswers): BankDetails =
    answers.get(BankDetailsPage) match {
      case Some(bankDetails) => bankDetails
      case _ =>
        logger.error("User has not submitted bank details")
        throw new Exception("User must supply bank details")
    }

  implicit class ImprovedOption[A](option: Option[A]) {
    def getOrError: A = {
      option.getOrElse {
        val exception = new Exception(s"Couldn't get expected value for type")
        logger.error(exception.getMessage, exception)
        throw exception
      }
    }
  }
}

