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

package services.etmp

import logging.Logging
import models.etmp.{EtmpEuRegistrationDetails, TaxRefTraderID, TraderId, VatNumberTraderId}
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Index, InternationalAddress, UserAnswers}
import pages.euDetails._
import queries.euDetails.AllEuDetailsRawQuery

trait EtmpEuRegistrations extends Logging {

  def getEuTaxRegistrations(answers: UserAnswers): List[EtmpEuRegistrationDetails] = {
    answers.get(TaxRegisteredInEuPage) match {
      case Some(true) =>
        answers.get(AllEuDetailsRawQuery) match {
          case Some(euDetails) =>
            euDetails.value.zipWithIndex.map {
              case (_, index) =>
                processEuDetails(answers, Index(index))
            }.toList
          case None =>
            val exception = new IllegalStateException("User must provide Eu details when tax registered n the EU")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case Some(false) =>
        List.empty
      case _ =>
        val exception = new IllegalStateException("User must answer if they are tax registered in the EU")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def processEuDetails(answers: UserAnswers, index: Index): EtmpEuRegistrationDetails = {
    answers.get(EuCountryPage(index)) match {
      case Some(country) =>
        answers.get(SellsGoodsToEuConsumerMethodPage(index)) match {
          case Some(EuConsumerSalesMethod.FixedEstablishment) =>
            val traderId = getTraderId(answers, index)
            val fixedEstablishmentTradingName = getFixedEstablishmentTradingName(answers, index)
            val fixedEstablishmentAddress = getFixedEstablishmentAddress(answers, index)
            EtmpEuRegistrationDetails(
              country.code,
              traderId,
              fixedEstablishmentTradingName,
              fixedEstablishmentAddress.line1,
              fixedEstablishmentAddress.line2,
              fixedEstablishmentAddress.townOrCity,
              fixedEstablishmentAddress.stateOrRegion,
              fixedEstablishmentAddress.postCode
            )
          case _ =>
            val exception = new IllegalStateException("As an IOSS trader the user cannot sell goods from a dispatch warehouse")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case None =>
        val exception = new IllegalStateException("User must select an EU country from which they sell")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getTraderId(answers: UserAnswers, index: Index): TraderId = {
    answers.get(RegistrationTypePage(index)) match {
      case Some(RegistrationType.VatNumber) =>
        answers.get(EuVatNumberPage(index)) match {
          case Some(euVatNumber) =>
            VatNumberTraderId(vatNumber = euVatNumber)
          case _ =>
            val exception = new IllegalStateException(s"Must have an EU VAT number if your Registration type is ${RegistrationType.VatNumber}")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case Some(RegistrationType.TaxId) =>
        answers.get(EuTaxReferencePage(index)) match {
          case Some(euTaxReference) =>
            TaxRefTraderID(taxReferenceNumber = euTaxReference)
          case _ =>
            val exception = new IllegalStateException(s"Must have an EU Tax Reference number if your Registration type is ${RegistrationType.TaxId}")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case _ =>
        val exception = new IllegalStateException("User must select a Registration type")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getFixedEstablishmentTradingName(answers: UserAnswers, index: Index): String =
    answers.get(FixedEstablishmentTradingNamePage(index)) match {
      case Some(name) => name
      case None =>
        val exception = new IllegalStateException("User must provide a fixed establishment trading name")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def getFixedEstablishmentAddress(answers: UserAnswers, index: Index): InternationalAddress =
    answers.get(FixedEstablishmentAddressPage(index)) match {
      case Some(address) => address
      case None =>
        val exception = new IllegalStateException("User must provide a fixed establishment address")
        logger.error(exception.getMessage, exception)
        throw exception
    }
}

