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

import connectors.returns.VatReturnConnector
import logging.Logging
import models._
import models.domain.{PreviousSchemeNumbers, _}
import models.euDetails._
import models.previousRegistrations.PreviousRegistrationDetails
import models.requests.AuthenticatedDataRequest
import pages._
import pages.euDetails._
import pages.filters.BusinessBasedInNiPage
import pages.previousRegistrations._
import pages.tradingNames.HasTradingNamePage
import queries.euDetails.AllEuOptionalDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import queries.{AllEuOptionalDetailsQuery, AllTradingNames, AllWebsites}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RegistrationService @Inject()(
                                     dateService: DateService,
                                     periodService: PeriodService,
                                     vatReturnConnector: VatReturnConnector,
                                     clock: Clock
                                   ) extends Logging {

  def toUserAnswers(userId: String, registration: Registration, vatCustomerInfo: VatCustomerInfo)(implicit ec: ExecutionContext): Future[UserAnswers] = {

    val userAnswers = for {
      businessBasedInNiUA <- UserAnswers(userId,
        vatInfo = Some(vatCustomerInfo)
      ).set(BusinessBasedInNiPage, true)
      hasTradingNameUA <- businessBasedInNiUA.set(HasTradingNamePage, registration.tradingNames.nonEmpty)
      tradingNamesUA <- if (registration.tradingNames.nonEmpty) {
        hasTradingNameUA.set(AllTradingNames, registration.tradingNames.toList)
      } else {
        Try(hasTradingNameUA)
      }

      hasTaxRegisteredInEuUA <- tradingNamesUA.set(TaxRegisteredInEuPage, registration.euRegistrations.nonEmpty)
      euVatDetailsUA <- if (registration.euRegistrations.nonEmpty) {
        hasTaxRegisteredInEuUA.set(AllEuOptionalDetailsQuery, getEuRegistrationDetails(registration).toList)
      } else {
        Try(hasTaxRegisteredInEuUA)
      }

      bankDetails <- euVatDetailsUA.set(BankDetailsPage, registration.bankDetails)

      hasPreviousRegistrationsUA <- bankDetails.set(PreviouslyRegisteredPage, registration.previousRegistrations.nonEmpty)
      previousRegistrationsUA <- setDeterminePreviousRegistrationAnswers(registration, hasPreviousRegistrationsUA)

      previousRegistrations <- if (registration.previousRegistrations.nonEmpty) {
        previousRegistrationsUA.set(AllPreviousRegistrationsQuery, getPreviousRegistrations(registration).toList)
      } else {
        Try(previousRegistrationsUA)
      }

      contactDetails <- previousRegistrations.set(BusinessContactDetailsPage, registration.contactDetails)

    } yield contactDetails

    Future.fromTry(userAnswers)
  }

  private def getEuRegistrationDetails(registration: Registration): Seq[EuOptionalDetails] = {

    registration.euRegistrations map {
      case euVatRegistration: EuVatRegistration => getEuDetailsForEuVatRegistration(euVatRegistration)

      case registrationWithFE: RegistrationWithFixedEstablishment => getEuDetailsForRegistrationWithFixedEstablishment(registrationWithFE)

      case registrationWithoutFE: RegistrationWithoutFixedEstablishmentWithTradeDetails =>
        getEuDetailsForRegistrationWithoutFEWithTradeDetails(registrationWithoutFE)

      case registrationWithoutTaxID: RegistrationWithoutTaxId => getEuDetailsForRegistrationWithoutTaxId(registrationWithoutTaxID)

    }
  }

  private def getEuDetailsForEuVatRegistration(euVatRegistration: EuVatRegistration): EuOptionalDetails = {
    EuOptionalDetails(
      euCountry = euVatRegistration.country,
      sellsGoodsToEUConsumers = Some(false),
      vatRegistered = if (Some(euVatRegistration.vatNumber).isDefined) Some(true) else Some(false),
      sellsGoodsToEUConsumerMethod = None,
      registrationType = None,
      euVatNumber = Some(s"${euVatRegistration.country.code}${euVatRegistration.vatNumber}"),
      euTaxReference = None,
      fixedEstablishmentTradingName = None,
      fixedEstablishmentAddress = None,
      euSendGoodsTradingName = None,
      euSendGoodsAddress = None
    )
  }

  private def getEuDetailsForRegistrationWithFixedEstablishment(registrationWithFE: RegistrationWithFixedEstablishment): EuOptionalDetails = {
    EuOptionalDetails(
      euCountry = registrationWithFE.country,
      sellsGoodsToEUConsumers = Some(true),
      vatRegistered = None,
      sellsGoodsToEUConsumerMethod = Some(EuConsumerSalesMethod.FixedEstablishment),
      registrationType = getRegistrationType(registrationWithFE.taxIdentifier.identifierType),
      euVatNumber = if (registrationWithFE.taxIdentifier.identifierType == EuTaxIdentifierType.Vat) convertTaxIdentifierForAmend(registrationWithFE.taxIdentifier.value, registrationWithFE.country.code) else None,
      euTaxReference = if (registrationWithFE.taxIdentifier.identifierType == EuTaxIdentifierType.Other) registrationWithFE.taxIdentifier.value else None,
      fixedEstablishmentTradingName = Some(registrationWithFE.fixedEstablishment.tradingName),
      fixedEstablishmentAddress = Some(registrationWithFE.fixedEstablishment.address),
      euSendGoodsTradingName = None,
      euSendGoodsAddress = None
    )
  }

  private def getEuDetailsForRegistrationWithoutFEWithTradeDetails(
                                                                    registrationWithoutFE: RegistrationWithoutFixedEstablishmentWithTradeDetails
                                                                  ): EuOptionalDetails = {
    EuOptionalDetails(
      euCountry = registrationWithoutFE.country,
      sellsGoodsToEUConsumers = Some(true),
      vatRegistered = None,
      sellsGoodsToEUConsumerMethod = Some(EuConsumerSalesMethod.DispatchWarehouse),
      registrationType = getRegistrationType(registrationWithoutFE.taxIdentifier.identifierType),
      euVatNumber = if (registrationWithoutFE.taxIdentifier.identifierType == EuTaxIdentifierType.Vat) convertTaxIdentifierForAmend(registrationWithoutFE.taxIdentifier.value, registrationWithoutFE.country.code) else None,
      euTaxReference = if (registrationWithoutFE.taxIdentifier.identifierType == EuTaxIdentifierType.Other) registrationWithoutFE.taxIdentifier.value else None,
      fixedEstablishmentTradingName = None,
      fixedEstablishmentAddress = None,
      euSendGoodsTradingName = Some(registrationWithoutFE.tradeDetails.tradingName),
      euSendGoodsAddress = Some(registrationWithoutFE.tradeDetails.address)
    )
  }

  private def getEuDetailsForRegistrationWithoutTaxId(registrationWithoutTaxID: RegistrationWithoutTaxId): EuOptionalDetails = {
    EuOptionalDetails(
      euCountry = registrationWithoutTaxID.country,
      sellsGoodsToEUConsumers = Some(false),
      vatRegistered = Some(false),
      sellsGoodsToEUConsumerMethod = None,
      registrationType = None,
      euVatNumber = None,
      euTaxReference = None,
      fixedEstablishmentTradingName = None,
      fixedEstablishmentAddress = None,
      euSendGoodsTradingName = None,
      euSendGoodsAddress = None)
  }

  private def convertTaxIdentifierForAmend(identifier: Option[String], countryCode: String): Option[String] = {
    CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode) match {
      case Some(countryValidationDetails) =>
        identifier match {
          case value if value.get.matches(countryValidationDetails.vrnRegex) =>
            value
          case value =>
            Some(value.get.prependedAll(countryCode))
        }

      case _ =>
        logger.error("Error occurred while getting country code regex, unable to convert identifier")
        throw new IllegalStateException("Error occurred while getting country code regex, unable to convert identifier")
    }
  }

  private def getRegistrationType(identifierType: EuTaxIdentifierType): Option[RegistrationType] = {
    identifierType match {
      case EuTaxIdentifierType.Vat => Some(RegistrationType.VatNumber)
      case EuTaxIdentifierType.Other => Some(RegistrationType.TaxId)
    }
  }

  private def getPreviousRegistrations(registration: Registration): Seq[PreviousRegistrationDetails] = {
    registration.previousRegistrations map {
      case previousRegistration: PreviousRegistrationNew =>
        PreviousRegistrationDetails(
          previousEuCountry = previousRegistration.country,
          previousSchemesDetails = previousRegistration.previousSchemesDetails.map {
            previousSchemesDetails =>
              PreviousSchemeDetails(
                previousScheme = previousSchemesDetails.previousScheme,
                previousSchemeNumbers =
                  PreviousSchemeNumbers(
                    previousSchemesDetails.previousSchemeNumbers.previousSchemeNumber,
                    previousSchemesDetails.previousSchemeNumbers.previousIntermediaryNumber
                  ))
          }.toList
        )
      case legacyPreviousRegistration: PreviousRegistrationLegacy =>
        PreviousRegistrationDetails(
          previousEuCountry = legacyPreviousRegistration.country,
          previousSchemesDetails = Seq(PreviousSchemeDetails(
            previousScheme = PreviousScheme.OSSU,
            previousSchemeNumbers = PreviousSchemeNumbers(
              previousSchemeNumber = legacyPreviousRegistration.vatNumber,
              previousIntermediaryNumber = None
            )
          ))
        )
    }
  }

  private def setDeterminePreviousRegistrationAnswers(
                                                       registration: Registration,
                                                       userAnswers: UserAnswers
                                                     ): Try[UserAnswers] = {

    recursivelySetPreviousSchemeUserAnswers(registration.previousRegistrations, userAnswers, 0)

  }

  private def recursivelySetPreviousSchemeUserAnswers(
                                                       remainingPreviousRegistration: Seq[PreviousRegistration],
                                                       currentUserAnswers: UserAnswers,
                                                       countryIndex: Int
                                                     ): Try[UserAnswers] = {
    remainingPreviousRegistration match {
      case Nil => Try(currentUserAnswers)
      case (firstPreviousRegistration: PreviousRegistrationNew) :: Nil =>
        recursivelySetPreviousSchemeDetails(firstPreviousRegistration.previousSchemesDetails, currentUserAnswers, countryIndex, 0)
      case (_: PreviousRegistrationLegacy) :: Nil => setLegacyPreviousRegistration(currentUserAnswers, countryIndex)
      case (firstPreviousRegistration: PreviousRegistrationNew) :: otherPreviousRegistrations => for {
        updatedUserAnswers <- recursivelySetPreviousSchemeDetails(firstPreviousRegistration.previousSchemesDetails, currentUserAnswers, countryIndex, 0)
        finalUpdatedUserAnswers <- recursivelySetPreviousSchemeUserAnswers(otherPreviousRegistrations, updatedUserAnswers, countryIndex + 1)
      } yield finalUpdatedUserAnswers
      case (_: PreviousRegistrationLegacy) :: otherPreviousRegistrations => for {
        updatedUserAnswers <- setLegacyPreviousRegistration(currentUserAnswers, countryIndex)
        finalUpdatedUserAnswers <- recursivelySetPreviousSchemeUserAnswers(otherPreviousRegistrations, updatedUserAnswers, countryIndex + 1)
      } yield finalUpdatedUserAnswers
    }
  }

  private def setLegacyPreviousRegistration(userAnswers: UserAnswers, countryIndex: Int): Try[UserAnswers] = {
    for {
      answers <- userAnswers.set(PreviousSchemeTypePage(Index(countryIndex), Index(0)), PreviousSchemeType.OSS)
      updatedAnswers <- answers.set(PreviousSchemePage(Index(countryIndex), Index(0)), PreviousScheme.OSSU)
    } yield updatedAnswers
  }

  private def settingOfPreviousSchemeDetailsUserAnswers(
                                                         currentUserAnswers: UserAnswers,
                                                         schemeDetails: PreviousSchemeDetails,
                                                         countryIndex: Int,
                                                         schemeIndex: Int
                                                       ): Try[UserAnswers] = {
    for {
      answers <- currentUserAnswers.set(PreviousSchemeTypePage(Index(countryIndex), Index(schemeIndex)), determineSchemeType(schemeDetails))
      updatedAnswers <- answers.set(PreviousSchemePage(Index(countryIndex), Index(schemeIndex)), schemeDetails.previousScheme)
      previousSchemeNumbers = schemeDetails.previousSchemeNumbers
      schemeType = updatedAnswers.get(PreviousSchemeTypePage(Index(countryIndex), Index(schemeIndex)))
      finalUserAnswers <-
        if (previousSchemeNumbers.previousIntermediaryNumber.nonEmpty && schemeType.contains(PreviousSchemeType.IOSS)) {
          updatedAnswers.set(PreviousIossSchemePage(Index(countryIndex), Index(schemeIndex)), true)
        } else {
          Try(updatedAnswers)
        }
    } yield finalUserAnswers
  }

  private def recursivelySetPreviousSchemeDetails(
                                                   remainingSchemeDetails: Seq[PreviousSchemeDetails],
                                                   currentUserAnswers: UserAnswers,
                                                   countryIndex: Int,
                                                   schemeIndex: Int
                                                 ): Try[UserAnswers] = {
    remainingSchemeDetails match {
      case Nil => Try(currentUserAnswers)
      case firstSchemeDetails :: Nil => settingOfPreviousSchemeDetailsUserAnswers(currentUserAnswers, firstSchemeDetails, countryIndex, schemeIndex)
      case firstSchemeDetails :: otherSchemeDetails =>
        for {
          updatedUserAnswers <- settingOfPreviousSchemeDetailsUserAnswers(currentUserAnswers, firstSchemeDetails, countryIndex, schemeIndex)
          finalUpdateUserAnswers <- recursivelySetPreviousSchemeDetails(otherSchemeDetails, updatedUserAnswers, countryIndex, schemeIndex + 1)
        } yield finalUpdateUserAnswers
    }
  }

  private def determineSchemeType(previousSchemeDetails: PreviousSchemeDetails): PreviousSchemeType = {
    previousSchemeDetails.previousScheme match {
      case scheme if scheme == PreviousScheme.OSSU || scheme == PreviousScheme.OSSNU =>
        PreviousSchemeType.OSS
      case scheme if scheme == PreviousScheme.IOSSWI || scheme == PreviousScheme.IOSSWOI =>
        PreviousSchemeType.IOSS
    }
  }

  def isEligibleSalesAmendable()(implicit ec: ExecutionContext, hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Boolean] = {
    request.registration match {
      case Some(registration) =>
        val firstReturnPeriod = periodService.getFirstReturnPeriod(registration.commencementDate)
        vatReturnConnector.get(firstReturnPeriod).map {
          case Right(_) =>
            false
          case _ =>
            val today = LocalDate.now(clock)
            val finalDay = dateService.calculateFinalAmendmentDate(registration.commencementDate)
            finalDay.isAfter(today) || finalDay.isEqual(today)
        }
      case _ =>
        Future.successful(true)
    }
  }



  def getLastPossibleDateOfFirstSale(maybeRegistration: Option[Registration])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[LocalDate]] = {
    maybeRegistration match {
      case Some(registration) =>
        val firstReturnPeriod = periodService.getFirstReturnPeriod(registration.commencementDate)
        vatReturnConnector.get(firstReturnPeriod).map {
          case Right(_) =>
            Some(firstReturnPeriod.lastDay)
          case _ =>
            None
        }
      case _ =>
        Future.successful(None)
    }
  }

}
