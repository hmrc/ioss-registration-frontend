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

import cats.implicits._
import logging.Logging
import models._
import models.domain._
import pages._
import pages.checkVatDetails.CheckVatDetailsPage
import pages.tradingNames.HasTradingNamePage
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.domain.Vrn

import javax.inject.Inject

class RegistrationValidationService @Inject()()
  extends EuTaxRegistrationValidations with PreviousRegistrationsValidations with Logging {


  def fromUserAnswers(answers: UserAnswers, vrn: Vrn): ValidationResult[Registration] = {
    (
      getCompanyName(answers),
      getTradingNames(answers),
      getVatDetails(answers),
      getEuTaxRegistrations(answers),
      getContactDetails(answers),
      getWebsites(answers),
      getPreviousRegistrations(answers),
      getBankDetails(answers)
    ).mapN(
      (
        name,
        tradingNames,
        vatDetails,
        euRegistrations,
        contactDetails,
        websites,
        previousRegistrations,
        bankDetails
      ) =>
        Registration(
          vrn = vrn,
          registeredCompanyName = name,
          tradingNames = tradingNames,
          vatDetails = vatDetails,
          euRegistrations = euRegistrations,
          contactDetails = contactDetails,
          websites = websites,
          previousRegistrations = previousRegistrations,
          bankDetails = bankDetails
        )
    )
  }


  private def getCompanyName(answers: UserAnswers): ValidationResult[String] =
    answers.vatInfo match {
      case Some(vatInfo) =>
        vatInfo.organisationName match {
          case Some(organisationName) => organisationName.validNec
          case _ =>
            vatInfo.individualName match {
              case Some(individualName) => individualName.validNec
              case _ => DataMissingError(CheckVatDetailsPage).invalidNec
            }
        }
      case _ => DataMissingError(CheckVatDetailsPage).invalidNec
    }

  private def getTradingNames(answers: UserAnswers): ValidationResult[List[TradingName]] = {
    answers.get(HasTradingNamePage) match {
      case Some(true) =>
        answers.get(AllTradingNames) match {
          case Some(Nil) | None => DataMissingError(AllTradingNames).invalidNec
          case Some(list) => list.validNec
        }

      case Some(false) =>
        answers.get(AllTradingNames) match {
          case Some(Nil) | None => List.empty.validNec
          case Some(_) => DataMissingError(HasTradingNamePage).invalidNec
        }

      case None =>
        DataMissingError(HasTradingNamePage).invalidNec
    }
  }

  private def getVatDetails(answers: UserAnswers): ValidationResult[VatDetails] = {
    answers.vatInfo.map(
      (vatInfo: VatCustomerInfo) =>
        VatDetails(
          vatInfo.registrationDate,
          vatInfo.desAddress,
          vatInfo.partOfVatGroup,
          VatDetailSource.Etmp
        ).validNec
    ).getOrElse(
      DataMissingError(CheckVatDetailsPage).invalidNec
    )
  }

  private def getContactDetails(answers: UserAnswers): ValidationResult[BusinessContactDetails] =
    answers.get(BusinessContactDetailsPage) match {
      case Some(details) => details.validNec
      case None => DataMissingError(BusinessContactDetailsPage).invalidNec
    }

  private def getBankDetails(answers: UserAnswers): ValidationResult[BankDetails] =
    answers.get(BankDetailsPage) match {
      case Some(bankDetails) => bankDetails.validNec
      case None => DataMissingError(BankDetailsPage).invalidNec
    }

  private def getWebsites(answers: UserAnswers): ValidationResult[List[String]] = {
    ???
  }
}
