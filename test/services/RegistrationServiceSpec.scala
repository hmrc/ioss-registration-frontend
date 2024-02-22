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

import base.SpecBase
import connectors.RegistrationConnector
import models.{BankDetails, BusinessContactDetails, InternationalAddress, PreviousScheme, TradingName, UserAnswers, Website}
import models.Country.euCountries
import models.amend.RegistrationWrapper
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.etmp._
import models.euDetails.{EuConsumerSalesMethod, EuDetails, RegistrationType}
import models.previousRegistrations.PreviousRegistrationDetails
import models.responses.etmp.EtmpEnrolmentResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import pages.euDetails.TaxRegisteredInEuPage
import pages.filters.BusinessBasedInNiPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import play.api.test.Helpers.running
import queries.AllWebsites
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import testutils.RegistrationData.{amendRegistrationResponse, etmpDisplayRegistration}
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

class RegistrationServiceSpec extends SpecBase with WireMockHelper with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)
  private val etmpRegistration = etmpDisplayRegistration


  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".createRegistration" - {

    "must create a registration request from user answers provided and return a successful ETMP enrolment response" in {

      val etmpEnrolmentResponse: EtmpEnrolmentResponse =
        EtmpEnrolmentResponse(iossReference = arbitrary[TaxRefTraderID].sample.value.taxReferenceNumber)

      when(mockRegistrationConnector.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder(Some(completeUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {

        registrationService.createRegistration(completeUserAnswersWithVatInfo, vrn).futureValue mustBe Right(etmpEnrolmentResponse)
        verify(mockRegistrationConnector, times(1)).createRegistration(any())(any())
      }
    }
  }

  ".amendRegistration" - {

    "must create a registration request from user answers provided and return a successful response" in {
      when(mockRegistrationConnector.amendRegistration(any())(any())) thenReturn Right(amendRegistrationResponse).toFuture

      val app = applicationBuilder(Some(completeUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {
        registrationService.amendRegistration(
          answers = completeUserAnswersWithVatInfo,
          registration = etmpDisplayRegistration,
          vrn = vrn,
          iossNumber = iossNumber,
          rejoin = false
        ).futureValue mustBe Right(amendRegistrationResponse)
        verify(mockRegistrationConnector, times(1)).amendRegistration(any())(any())
      }
    }
  }

  ".toUserAnswers" - {

    val convertedTradingNames: List[TradingName] = for {
      tradingName <- etmpRegistration.tradingNames.toList
    } yield TradingName(name = tradingName.tradingName)

    def convertToPreviousRegistrationDetails(etmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]): Seq[PreviousRegistrationDetails] = {
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
    }

    def convertPreviousSchemeDetails(etmpPreviousEURegistrationDetails: EtmpPreviousEuRegistrationDetails): PreviousSchemeDetails = {
      val schemeType = convertedSchemeType(etmpPreviousEURegistrationDetails.schemeType)
      PreviousSchemeDetails(
        previousScheme = schemeType,
        previousSchemeNumbers = PreviousSchemeNumbers(
          previousSchemeNumber =
            if(schemeType == PreviousScheme.OSSU) {
              s"${etmpPreviousEURegistrationDetails.issuedBy}${etmpPreviousEURegistrationDetails.registrationNumber}"
            } else {
              etmpPreviousEURegistrationDetails.registrationNumber
            },
          previousIntermediaryNumber = etmpPreviousEURegistrationDetails.intermediaryNumber
        ),
        nonCompliantDetails = None
      )
    }

    def convertedSchemeType(schemeType: SchemeType): PreviousScheme = {
      schemeType match {
        case SchemeType.OSSUnion => PreviousScheme.OSSU
        case SchemeType.OSSNonUnion => PreviousScheme.OSSNU
        case SchemeType.IOSSWithIntermediary => PreviousScheme.IOSSWI
        case SchemeType.IOSSWithoutIntermediary => PreviousScheme.IOSSWOI
      }
    }

    val previousRegistrations = convertToPreviousRegistrationDetails(etmpRegistration.schemeDetails.previousEURegistrationDetails)

    def determineRegistrationType(vatNumber: Option[String], taxIdentificationNumber: Option[String]): Option[RegistrationType] =
      (vatNumber, taxIdentificationNumber) match {
        case (Some(_), _) => Some(RegistrationType.VatNumber)
        case _ => Some(RegistrationType.TaxId)
      }

    val euDetails: Seq[EuDetails] = for {
      euCountryDetails <- etmpRegistration.schemeDetails.euRegistrationDetails
    } yield {
      EuDetails(
        euCountry = euCountries.filter(_.code == euCountryDetails.issuedBy).head,
        sellsGoodsToEuConsumerMethod = Some(EuConsumerSalesMethod.FixedEstablishment),
        registrationType = determineRegistrationType(euCountryDetails.vatNumber, euCountryDetails.taxIdentificationNumber),
        euVatNumber = euCountryDetails.vatNumber.map(vatNumber => s"${euCountryDetails.issuedBy}$vatNumber"),
        euTaxReference = euCountryDetails.taxIdentificationNumber,
        fixedEstablishmentTradingName = Some(euCountryDetails.fixedEstablishmentTradingName),
        fixedEstablishmentAddress = Some(InternationalAddress(
          line1 = euCountryDetails.fixedEstablishmentAddressLine1,
          line2 = euCountryDetails.fixedEstablishmentAddressLine2,
          townOrCity = euCountryDetails.townOrCity,
          stateOrRegion = euCountryDetails.regionOrState,
          postCode = euCountryDetails.postcode,
          country = euCountries.filter(_.code == euCountryDetails.issuedBy).head
        ))
      )
    }

    val convertWebsites: List[Website] = for {
        website <- etmpRegistration.schemeDetails.websites.toList
      } yield Website(site = website.websiteAddress)

    val convertedBankDetails: BankDetails = {
      val etmpBankDetails = etmpRegistration.bankDetails
      BankDetails(
        accountName = etmpBankDetails.accountName,
        bic = etmpBankDetails.bic,
        iban = etmpBankDetails.iban
      )
    }

    val contactDetails: BusinessContactDetails = {
      val etmpSchemeDetails = etmpRegistration.schemeDetails
      BusinessContactDetails(
        fullName = etmpSchemeDetails.contactName,
        telephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
        emailAddress = etmpSchemeDetails.businessEmailId
      )
    }

    "unwrap received registration into user answers" in {

      val userAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(BusinessBasedInNiPage, true).success.value
        .set(HasTradingNamePage, true).success.value
        .set(AllTradingNames, convertedTradingNames).success.value
        .set(PreviouslyRegisteredPage, true).success.value
        .set(AllPreviousRegistrationsQuery, previousRegistrations.toList).success.value
        .set(TaxRegisteredInEuPage, true).success.value
        .set(AllEuDetailsQuery, euDetails.toList).success.value
        .set(AllWebsites, convertWebsites).success.value
        .set(BusinessContactDetailsPage, contactDetails).success.value
        .set(BankDetailsPage, convertedBankDetails).success.value

      val receivedRegistrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpRegistration)

      val result = registrationService.toUserAnswers(userId = userAnswersId, registrationWrapper = receivedRegistrationWrapper).futureValue

      result mustBe userAnswers.copy(lastUpdated = result.lastUpdated)
    }
  }
}