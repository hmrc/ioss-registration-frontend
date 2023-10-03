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
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, DataMissingError, GenericError, Index, InternationalAddress, UserAnswers, ValidationResult}
import pages.euDetails._
import queries.AllEuDetailsRawQuery

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
      case Some(country) =>
        answers.get(SellsGoodsToEUConsumersPage(index)) match {
          case Some(true) =>
            sellsGoodsToEuConsumers(answers, country, index)
          case Some(false) =>
            doesNotSellGoodsToEuConsumers(answers, country, index)
          case None => DataMissingError(SellsGoodsToEUConsumersPage(index)).invalidNec
        }
      case None =>
        DataMissingError(EuCountryPage(index)).invalidNec
    }
  }

  private def sellsGoodsToEuConsumers(answers: UserAnswers, country: Country, index: Index): ValidationResult[EuTaxRegistration] = {
    (answers.vatInfo.exists(_.partOfVatGroup), answers.get(SellsGoodsToEUConsumerMethodPage(index))) match {
      case (true, Some(EuConsumerSalesMethod.DispatchWarehouse)) =>
        getRegistrationWithDispatchWarehouse(answers, country, index)
      case (true, Some(EuConsumerSalesMethod.FixedEstablishment)) =>
        GenericError("A state where a country exists but cannot be added").invalidNec
      case (true, None) =>
        DataMissingError(SellsGoodsToEUConsumerMethodPage(index)).invalidNec
      case (false, Some(EuConsumerSalesMethod.FixedEstablishment)) =>
        getRegistrationWithFixedEstablishment(answers, country, index)
      case (false, Some(EuConsumerSalesMethod.DispatchWarehouse)) =>
        getRegistrationWithDispatchWarehouse(answers, country, index)
      case (_, None) =>
        DataMissingError(SellsGoodsToEUConsumerMethodPage(index)).invalidNec
    }
  }

  private def doesNotSellGoodsToEuConsumers(answers: UserAnswers, country: Country, index: Index): ValidationResult[EuTaxRegistration] = {
    answers.get(VatRegisteredPage(index)) match {
      case Some(true) =>
        getEuVatNumber(answers, index).map(
          vatNumber => EuVatRegistration(country, vatNumber)
        )

      case _ =>
        DataMissingError(VatRegisteredPage(index)).invalidNec
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

  private def getRegistrationWithDispatchWarehouse(answers: UserAnswers, country: Country, index: Index): ValidationResult[EuTaxRegistration] =
    (
      getEuTaxIdentifier(answers, index),
      getEuSendGoods(answers, index)
      ).mapN(
      (taxIdentifier, dispatchWarehouse) =>
        RegistrationWithoutFixedEstablishmentWithTradeDetails(country, taxIdentifier, dispatchWarehouse)
    )

  private def getEuTaxIdentifier(answers: UserAnswers, index: Index): ValidationResult[EuTaxIdentifier] = {
    answers.get(SellsGoodsToEUConsumersPage(index)) match {
      case Some(true) =>
        answers.get(RegistrationTypePage(index)) match {
          case Some(RegistrationType.VatNumber) =>
            getEuVatNumber(answers, index).map(v => EuTaxIdentifier(Vat, Some(v)))
          case Some(RegistrationType.TaxId) =>
            getEuTaxId(answers, index).map(v => EuTaxIdentifier(Other, Some(v)))
          case None => DataMissingError(RegistrationTypePage(index)).invalidNec
        }

      case Some(false) =>
        answers.get(VatRegisteredPage(index)) match {
          case Some(true) =>
            getEuVatNumber(answers, index).map(v => EuTaxIdentifier(Vat, Some(v)))
          case Some(false) =>
            EuTaxIdentifier(Other, None).validNec
          case None =>
            DataMissingError(VatRegisteredPage(index)).invalidNec
        }

      case None => DataMissingError(SellsGoodsToEUConsumersPage(index)).invalidNec
    }
  }

  private def getEuVatNumber(answers: UserAnswers, index: Index): ValidationResult[String] =
    answers.get(EuVatNumberPage(index)) match {
      case Some(vatNumber) => vatNumber.validNec
      case None => DataMissingError(EuVatNumberPage(index)).invalidNec
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

  private def getEuSendGoods(answers: UserAnswers, index: Index): ValidationResult[TradeDetails] =
    (
      getEuSendGoodsTradingName(answers, index),
      getEuSendGoodsAddress(answers, index)
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

  private def getEuSendGoodsTradingName(userAnswers: UserAnswers, index: Index): ValidationResult[String] = {
    userAnswers.get(EuSendGoodsTradingNamePage(index)) match {
      case Some(answer) => answer.validNec
      case None => DataMissingError(EuSendGoodsTradingNamePage(index)).invalidNec
    }
  }

  private def getEuSendGoodsAddress(userAnswers: UserAnswers, index: Index): ValidationResult[InternationalAddress] = {
    userAnswers.get(EuSendGoodsAddressPage(index)) match {
      case Some(answer) => answer.validNec
      case None => DataMissingError(EuSendGoodsAddressPage(index)).invalidNec
    }
  }

}
