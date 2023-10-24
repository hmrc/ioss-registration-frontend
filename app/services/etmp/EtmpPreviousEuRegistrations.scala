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
import models.domain.{PreviousRegistration, PreviousSchemeDetails, PreviousSchemeNumbers}
import models.etmp.{EtmpPreviousEuRegistrationDetails, SchemeType}
import models.{Country, CountryWithValidationDetails, Index, PreviousScheme, UserAnswers}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage, PreviouslyRegisteredPage}
import queries.previousRegistration.{AllPreviousRegistrationsRawQuery, AllPreviousSchemesRawQuery}

trait EtmpPreviousEuRegistrations extends Logging {

  def getPreviousRegistrationDetails(answers: UserAnswers): Seq[EtmpPreviousEuRegistrationDetails] = {
    answers.get(PreviouslyRegisteredPage) match {
      case Some(true) =>
        answers.get(AllPreviousRegistrationsRawQuery) match {
          case Some(previousEuRegistrations) =>
            previousEuRegistrations.value.zipWithIndex.flatMap {
              case (_, index) =>
                processPreviousEuRegistrationDetails(answers, Index(index))
            }.toList
          case None =>
            val exception = new IllegalStateException("User must provide previous Eu details when previously tax registered in the EU")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case Some(false) =>
        List.empty
      case _ =>
        val exception = new IllegalStateException("User must answer if they are previously tax registered in the EU")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def processPreviousEuRegistrationDetails(answers: UserAnswers, countryIndex: Index): Seq[EtmpPreviousEuRegistrationDetails] = {
    answers.get(PreviousEuCountryPage(countryIndex)) match {
      case Some(previousEuCountry) =>
        val previousRegistration = getPreviousRegistration(answers, previousEuCountry, countryIndex)
        previousRegistration.previousSchemesDetails.map { previousSchemeDetails =>

          val registrationNumber: String = previousSchemeDetails.previousScheme match {
            case PreviousScheme.OSSU => CountryWithValidationDetails.convertTaxIdentifierForTransfer(
              previousSchemeDetails.previousSchemeNumbers.previousSchemeNumber, previousEuCountry.code
            )
            case _ => previousSchemeDetails.previousSchemeNumbers.previousSchemeNumber
          }

          EtmpPreviousEuRegistrationDetails(
            issuedBy = previousEuCountry.code,
            registrationNumber = registrationNumber,
            schemeType = convertSchemeType(previousSchemeDetails.previousScheme),
            intermediaryNumber = previousSchemeDetails.previousSchemeNumbers.previousIntermediaryNumber
          )
        }
      case None =>
        val exception = new IllegalStateException("User must select a previous EU country from which they sold from")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getPreviousRegistration(answers: UserAnswers, country: Country, countryIndex: Index): PreviousRegistration = {
    val previousSchemeDetails = getPreviousSchemes(answers, countryIndex)
    PreviousRegistration(country, previousSchemeDetails)
  }

  private def getPreviousSchemes(answers: UserAnswers, countryIndex: Index): Seq[PreviousSchemeDetails] = {
    answers.get(AllPreviousSchemesRawQuery(countryIndex)) match {
      case Some(previousSchemes) =>
        previousSchemes.value.zipWithIndex.map {
          case (_, schemeIndex) =>
            processPreviousSchemes(answers, countryIndex, Index(schemeIndex))
        }.toList
      case None =>
        List.empty
    }
  }

  private def processPreviousSchemes(answers: UserAnswers, countryIndex: Index, schemeIndex: Index): PreviousSchemeDetails = {
    val previousScheme = getPreviousScheme(answers, countryIndex, schemeIndex)
    val previousSchemeNumber = getPreviousSchemeNumber(answers, countryIndex, schemeIndex)
    PreviousSchemeDetails(previousScheme, previousSchemeNumber)
  }

  private def getPreviousScheme(answers: UserAnswers, countryIndex: Index, schemeIndex: Index): PreviousScheme =
    answers.get(PreviousSchemePage(countryIndex, schemeIndex)) match {
      case Some(previousScheme) => previousScheme
      case None =>
        val exception = new IllegalStateException("A previous scheme must be provided")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def getPreviousSchemeNumber(answers: UserAnswers, countryIndex: Index, schemeIndex: Index): PreviousSchemeNumbers =
    answers.get(PreviousOssNumberPage(countryIndex, schemeIndex)) match {
      case Some(previousSchemeNumber) => previousSchemeNumber
      case None =>
        val exception = new IllegalStateException("User must provide a previous scheme number")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def convertSchemeType(schemeType: PreviousScheme): SchemeType =
    schemeType match {
      case PreviousScheme.OSSU => SchemeType.OSSUnion
      case PreviousScheme.OSSNU => SchemeType.OSSNonUnion
      case PreviousScheme.IOSSWI => SchemeType.IOSSWithIntermediary
      case PreviousScheme.IOSSWOI => SchemeType.IOSSWithoutIntermediary
      case _ => throw new Exception("Unknown scheme type, unable to convert")
    }
}

