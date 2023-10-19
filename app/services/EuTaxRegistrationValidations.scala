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
import models.domain.EuTaxIdentifierType.{Other, Vat}
import models.domain._
import models.{Country, CountryWithValidationDetails, DataMissingError, Index, InternationalAddress, UserAnswers, ValidationError, ValidationResult}
import pages.euDetails._
import queries.euDetails.AllEuDetailsRawQuery

trait EuTaxRegistrationValidations {


  def getEuTaxRegistrations(answers: UserAnswers): ValidationResult[List[EuTaxRegistration]] = {
    answers.get(TaxRegisteredInEuPage) match {
      case Some(true) =>
        answers.get(AllEuDetailsRawQuery) match {
          case None =>
            DataMissingError(AllEuDetailsRawQuery).invalidNec
          case Some(euDetails) =>
            euDetails.value.zipWithIndex.map {
              case (_, index) =>
                processEuDetail(answers, Index(index))
            }.toList.sequence
        }

      case Some(false) =>
        answers.get(AllEuDetailsRawQuery) match {
          case Some(_) => DataMissingError(AllEuDetailsRawQuery).invalidNec
          case None => List.empty.validNec
        }

      case None =>
        DataMissingError(TaxRegisteredInEuPage).invalidNec
    }
  }

  private def processEuDetail(answers: UserAnswers, index: Index): ValidationResult[EuTaxRegistration] = {
    answers.get(EuCountryPage(index)) match {
      case Some(country) => getRegistrationWithFixedEstablishment(answers, country, index)
      case None => DataMissingError(EuCountryPage(index)).invalidNec
    }
  }

  private def getRegistrationWithFixedEstablishment(answers: UserAnswers, country: Country, index: Index): ValidationResult[EuTaxRegistration] =
    (
      getEuTaxIdentifier(answers, index),
      getFixedEstablishment(answers, index)
    ).mapN(
      (taxIdentifier, fixedEstablishment) =>
        RegistrationWithFixedEstablishment(country, taxIdentifier, fixedEstablishment)
    )

  private def getEuVatNumber(answers: UserAnswers, index: Index): ValidationResult[String] = {
    val country = answers.get(EuCountryPage(index))
    val euVatNumber = answers.get(EuVatNumberPage(index))
    (euVatNumber, country) match {
      case (Some(vatNumber), Some(country)) =>
        CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == country.code) match {
          case Some(validationRule) if vatNumber.matches(validationRule.vrnRegex) =>
            vatNumber.validNec
          case _ =>
            DataMissingError(EuVatNumberPage(index)).invalidNec
        }
      case (None, _) => DataMissingError(EuVatNumberPage(index)).invalidNec
    }
  }

  private def getEuTaxIdentifier(answers: UserAnswers, index: Index): ValidationResult[EuTaxIdentifier] = {
    getEuVatNumber(answers, index).map(v => EuTaxIdentifier(Vat, Some(v)))
      .orElse(getEuTaxId(answers, index).map(v => EuTaxIdentifier(Other, Some(v))))
      .orElse(EuTaxIdentifier(Other, None).validNec[ValidationError])
  }

  private def getEuTaxId(answers: UserAnswers, index: Index): ValidationResult[String] =
    answers.get(EuTaxReferencePage(index)) match {
      case Some(taxId) => taxId.validNec
      case None => DataMissingError(EuTaxReferencePage(index)).invalidNec
    }

  private def getFixedEstablishment(answers: UserAnswers, index: Index): ValidationResult[TradeDetails] =
    (
      getFixedEstablishmentTradingName(answers, index),
      getFixedEstablishmentAddress(answers, index)
    ).mapN(TradeDetails.apply)

  private def getFixedEstablishmentTradingName(answers: UserAnswers, index: Index): ValidationResult[String] =
    answers.get(FixedEstablishmentTradingNamePage(index)) match {
      case Some(name) => name.validNec
      case None => DataMissingError(FixedEstablishmentTradingNamePage(index)).invalidNec
    }

  private def getFixedEstablishmentAddress(answers: UserAnswers, index: Index): ValidationResult[InternationalAddress] =
    answers.get(FixedEstablishmentAddressPage(index)) match {
      case Some(address) => address.validNec
      case None => DataMissingError(FixedEstablishmentAddressPage(index)).invalidNec
    }
}
