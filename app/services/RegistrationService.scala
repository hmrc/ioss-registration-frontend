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
import connectors.RegistrationHttpParser.{AmendRegistrationResultResponse, RegistrationResultResponse}
import logging.Logging
import models.Country.euCountries
import models.amend.RegistrationWrapper
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import models.etmp._
import models.euDetails.{EuConsumerSalesMethod, EuDetails, RegistrationType}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{BankDetails, BusinessContactDetails, Country, InternationalAddress, PreviousScheme, TradingName, UserAnswers, Website}
import models.etmp.amend.EtmpAmendRegistrationRequest.buildEtmpAmendRegistrationRequest
import pages.euDetails.TaxRegisteredInEuPage
import pages.filters.BusinessBasedInNiPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import queries.AllWebsites
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import services.etmp.{EtmpEuRegistrations, EtmpPreviousEuRegistrations}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.Future
import scala.util.Try

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {

  def createRegistration(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    val commencementDate = LocalDateTime.now(clock)
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, vrn, commencementDate))
  }

  def amendRegistration(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    val commencementDate = LocalDateTime.now(clock)
    registrationConnector.amendRegistration(buildEtmpAmendRegistrationRequest(answers, vrn, commencementDate))
  }

  def toUserAnswers(userId: String, registrationWrapper: RegistrationWrapper): Future[UserAnswers] = {

    val etmpTradingNames = registrationWrapper.registration.tradingNames
    val etmpPreviousRegistrations = registrationWrapper.registration.schemeDetails.previousEURegistrationDetails
    val etmpEuDetails = registrationWrapper.registration.schemeDetails.euRegistrationDetails
    val etmpWebsites = registrationWrapper.registration.schemeDetails.websites
    val schemeDetails = registrationWrapper.registration.schemeDetails
    val etmpBankDetails = registrationWrapper.registration.bankDetails

    val userAnswers = for {
      businessBasedInNi <- UserAnswers(
        id = userId,
        vatInfo = Some(registrationWrapper.vatInfo)
      ).set(BusinessBasedInNiPage, true)

      hasTradingNamesUA <- businessBasedInNi.set(HasTradingNamePage, etmpTradingNames.nonEmpty)
      tradingNamesUA <- if (etmpTradingNames.nonEmpty) {
        hasTradingNamesUA.set(AllTradingNames, convertToTradingNames(etmpTradingNames).toList)
      } else {
        Try(hasTradingNamesUA)
      }

      hasPreviousRegistrationsUA <- tradingNamesUA.set(PreviouslyRegisteredPage, etmpPreviousRegistrations.nonEmpty)
      previousRegistrationsUA <- if (etmpPreviousRegistrations.nonEmpty) {
        hasPreviousRegistrationsUA.set(AllPreviousRegistrationsQuery, convertToPreviousRegistrationDetails(etmpPreviousRegistrations).toList)
      } else {
        Try(hasPreviousRegistrationsUA)
      }

      hasTaxRegisteredInEuUA <- previousRegistrationsUA.set(TaxRegisteredInEuPage, etmpEuDetails.nonEmpty)
      taxRegistrationsInEuUA <- if (etmpEuDetails.nonEmpty) {
        hasTaxRegisteredInEuUA.set(AllEuDetailsQuery, convertToEuDetails(etmpEuDetails).toList)
      } else {
        Try(hasTaxRegisteredInEuUA)
      }

      websitesUA <- taxRegistrationsInEuUA.set(AllWebsites, convertWebsite(etmpWebsites).toList)

      contactDetails <- websitesUA.set(BusinessContactDetailsPage, getBusinessContactDetails(schemeDetails))

      bankDetails <- contactDetails.set(BankDetailsPage, convertBankDetails(etmpBankDetails))

    } yield bankDetails

    Future.fromTry(userAnswers)
  }

  private def convertToTradingNames(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] =
    for {
      tradingName <- etmpTradingNames.map(_.tradingName)
    } yield TradingName(name = tradingName)

  private def convertToPreviousRegistrationDetails(
                                                    etmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]
                                                  ): Seq[PreviousRegistrationDetails] =
    for {
      issuedBy <- etmpPreviousEuRegistrationDetails.map(_.issuedBy).distinct
    } yield {
      val country = euCountries.filter(_.code == issuedBy).head
      val schemeDetailsForCountry = etmpPreviousEuRegistrationDetails.filter(_.issuedBy == issuedBy)
      PreviousRegistrationDetails(
        previousEuCountry = country,
        previousSchemesDetails = schemeDetailsForCountry.map(convertPreviousSchemeDetails)
      )
    }

  private def convertPreviousSchemeDetails(etmpPreviousEURegistrationDetails: EtmpPreviousEuRegistrationDetails): PreviousSchemeDetails =
    PreviousSchemeDetails(
      previousScheme = convertedSchemeType(etmpPreviousEURegistrationDetails.schemeType),
      previousSchemeNumbers = PreviousSchemeNumbers(
        previousSchemeNumber = etmpPreviousEURegistrationDetails.registrationNumber,
        previousIntermediaryNumber = etmpPreviousEURegistrationDetails.intermediaryNumber
      )
    )

  private def convertedSchemeType(schemeType: SchemeType): PreviousScheme =
    schemeType match {
      case SchemeType.OSSUnion => PreviousScheme.OSSU
      case SchemeType.OSSNonUnion => PreviousScheme.OSSNU
      case SchemeType.IOSSWithIntermediary => PreviousScheme.IOSSWI
      case SchemeType.IOSSWithoutIntermediary => PreviousScheme.IOSSWOI
      case _ => throw new IllegalStateException("Not a recognised scheme type")
    }

  private def convertToEuDetails(etmpEuRegistrationDetails: Seq[EtmpEuRegistrationDetails]): Seq[EuDetails] =
    for {
      etmpPreviousEuCountryRegistrationDetails <- etmpEuRegistrationDetails
    } yield {
      EuDetails(
        euCountry = getCountry(etmpPreviousEuCountryRegistrationDetails.countryOfRegistration),
        sellsGoodsToEuConsumerMethod = Some(EuConsumerSalesMethod.FixedEstablishment),
        registrationType = determineRegistrationType(etmpPreviousEuCountryRegistrationDetails.traderId),
        euVatNumber = getVatNumber(etmpPreviousEuCountryRegistrationDetails.traderId),
        euTaxReference = getTaxId(etmpPreviousEuCountryRegistrationDetails.traderId),
        fixedEstablishmentTradingName = Some(etmpPreviousEuCountryRegistrationDetails.tradingName),
        fixedEstablishmentAddress = Some(InternationalAddress(
          line1 = etmpPreviousEuCountryRegistrationDetails.fixedEstablishmentAddressLine1,
          line2 = etmpPreviousEuCountryRegistrationDetails.fixedEstablishmentAddressLine2,
          townOrCity = etmpPreviousEuCountryRegistrationDetails.townOrCity,
          stateOrRegion = etmpPreviousEuCountryRegistrationDetails.regionOrState,
          postCode = etmpPreviousEuCountryRegistrationDetails.postcode,
          country = getCountry(etmpPreviousEuCountryRegistrationDetails.countryOfRegistration
          ))
        )
      )
    }

  private def getVatNumber(traderId: TraderId): Option[String] =
    traderId match {
      case vatNumberTraderId: VatNumberTraderId =>
        Some(vatNumberTraderId.vatNumber)
      case _ => None
    }

  private def getTaxId(traderId: TraderId): Option[String] =
    traderId match {
      case taxRefTraderID: TaxRefTraderID =>
        Some(taxRefTraderID.taxReferenceNumber)
      case _ => None
    }

  private def determineRegistrationType(traderId: TraderId): Option[RegistrationType] =
    traderId match {
      case _: VatNumberTraderId => Some(RegistrationType.VatNumber)
      case _: TaxRefTraderID => Some(RegistrationType.TaxId)
    }

  private def getCountry(countryCode: String): Country =
    Country.euCountries.find(_.code == countryCode) match {
      case Some(country) => country
      case _ =>
        val exception = new IllegalStateException(s"Unable to find country $countryCode")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  private def convertWebsite(etmpWebsites: Seq[EtmpWebsite]): Seq[Website] =
    for {
      etmpWebsite <- etmpWebsites.toList
    } yield Website(site = etmpWebsite.websiteAddress)

  private def getBusinessContactDetails(schemeDetails: EtmpSchemeDetails): BusinessContactDetails =
    BusinessContactDetails(
      fullName = schemeDetails.contactName,
      telephoneNumber = schemeDetails.businessTelephoneNumber,
      emailAddress = schemeDetails.businessEmailId
    )

  private def convertBankDetails(etmpBankDetails: EtmpBankDetails): BankDetails =
    BankDetails(
      accountName = etmpBankDetails.accountName,
      bic = etmpBankDetails.bic,
      iban = etmpBankDetails.iban
    )
}

