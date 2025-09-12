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

package models.etmp

import base.SpecBase
import config.Constants.{maxSchemes, maxTradingNames, maxWebsites}
import config.FrontendAppConfig
import formats.Format.eisDateFormatter
import models.domain.PreviousSchemeDetails
import models.euDetails.{EuDetails, RegistrationType}
import models.previousRegistrations.{NonCompliantDetails, PreviousRegistrationDetails}
import models.{BankDetails, Bic, BusinessContactDetails, CountryWithValidationDetails, Iban, PreviousScheme, TradingName, UserAnswers, Website}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails.TaxRegisteredInEuPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import play.api.libs.json.{JsError, Json, JsSuccess}
import queries.AllWebsites
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import testutils.RegistrationData.etmpRegistrationRequest

import java.time.LocalDate

class EtmpRegistrationRequestSpec extends SpecBase with MockitoSugar {

  private val mockFrontendAppConfig = mock[FrontendAppConfig]

  private val administration = etmpRegistrationRequest.administration
  private val customerIdentification = etmpRegistrationRequest.customerIdentification
  private val tradingNames = etmpRegistrationRequest.tradingNames
  private val schemeDetails = etmpRegistrationRequest.schemeDetails
  private val bankDetails = etmpRegistrationRequest.bankDetails

  private val numberOfRegistrations: Int = 8

  private def convertSchemeType(schemeType: PreviousScheme): SchemeType =
    schemeType match {
      case PreviousScheme.OSSU => SchemeType.OSSUnion
      case PreviousScheme.OSSNU => SchemeType.OSSNonUnion
      case PreviousScheme.IOSSWI => SchemeType.IOSSWithIntermediary
      case PreviousScheme.IOSSWOI => SchemeType.IOSSWithoutIntermediary
    }

  private def convertToTraderId(euDetails: EuDetails): Option[TraderId] = {
    euDetails.registrationType match {
      case Some(RegistrationType.VatNumber) =>
        val convertedVatNumber = CountryWithValidationDetails.convertTaxIdentifierForTransfer(euDetails.euVatNumber.value, euDetails.euCountry.code)
        Some(VatNumberTraderId(convertedVatNumber))
      case Some(RegistrationType.TaxId) =>
        Some(TaxRefTraderID(euDetails.euTaxReference.value))
      case _ => None
    }
  }

  "EtmpRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> administration,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )

      val expectedResult = EtmpRegistrationRequest(
        administration = administration,
        customerIdentification = customerIdentification,
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpRegistrationRequest] mustBe JsSuccess(expectedResult)
    }

    ".buildEtmpRegistrationRequest" - {

      val tradingNames: List[TradingName] = Gen.listOfN(maxTradingNames, arbitrary[TradingName]).sample.value
      val previousRegistration: PreviousRegistrationDetails = PreviousRegistrationDetails(
        previousEuCountry = arbitraryCountry.arbitrary.sample.value,
        previousSchemesDetails = Gen.listOfN(maxSchemes, PreviousSchemeDetails(
          previousScheme = arbitraryPreviousScheme.arbitrary.sample.value,
          previousSchemeNumbers = arbitraryPreviousIossSchemeDetails.arbitrary.sample.value,
          nonCompliantDetails = Some(arbitraryNonCompliantDetails.arbitrary.sample.value)
        )).sample.value
      )
      val previousEuRegistrations: List[PreviousRegistrationDetails] = Gen.listOfN(numberOfRegistrations, previousRegistration).sample.value
      val euRegistration: EuDetails = EuDetails(
        euCountry = arbitraryCountry.arbitrary.sample.value,
        hasFixedEstablishment = Some(true),
        registrationType = Some(arbitraryRegistrationType.arbitrary.sample.value),
        euVatNumber = Some(arbitraryEuVatNumber.sample.value),
        euTaxReference = Some(arbitraryEuTaxReference.sample.value),
        fixedEstablishmentTradingName = Some(arbitraryFixedEstablishmentTradingNamePage.arbitrary.sample.value.toString),
        fixedEstablishmentAddress = Some(arbitraryInternationalAddress.arbitrary.sample.value)
      )
      val euRegistrations = Gen.listOfN(numberOfRegistrations, euRegistration).sample.value
      val websites = Gen.listOfN(maxWebsites, arbitrary[Website]).sample.value
      val bankDetails = BankDetails(
        accountName = arbitrary[String].sample.value,
        bic = Some(arbitrary[Bic].sample.value),
        iban = arbitrary[Iban].sample.value
      )
      val genBusinessContactDetails = BusinessContactDetails(
        fullName = arbitrary[String].sample.value,
        telephoneNumber = arbitrary[String].sample.value,
        emailAddress = arbitrary[String].sample.value
      )

      val userAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(HasTradingNamePage, true).success.value
        .set(AllTradingNames, tradingNames).success.value
        .set(PreviouslyRegisteredPage, true).success.value
        .set(AllPreviousRegistrationsQuery, previousEuRegistrations).success.value
        .set(TaxRegisteredInEuPage, true).success.value
        .set(AllEuDetailsQuery, euRegistrations).success.value
        .set(AllWebsites, websites).success.value
        .set(BusinessContactDetailsPage, genBusinessContactDetails).success.value
        .set(BankDetailsPage, bankDetails).success.value

      "must convert userAnswers to an EtmpRegistrationRequest" in {
        
        when(mockFrontendAppConfig.release9Enabled) thenReturn true

        val convertToEtmpTradingNames: List[EtmpTradingName] =
          for {
            tradingName <- tradingNames
          } yield EtmpTradingName(tradingName.name)

        val convertToEtmpWebsite: List[EtmpWebsite] =
          for {
            website <- websites
          } yield EtmpWebsite(website.site)

        val convertToEtmpBankDetails: EtmpBankDetails =
          EtmpBankDetails(
            accountName = bankDetails.accountName,
            bic = bankDetails.bic,
            iban = bankDetails.iban
          )

        val getNonCompliantDetails: Option[NonCompliantDetails] = {
          val maybeNonCompliantDetailsList = previousEuRegistrations.flatMap(_.previousSchemesDetails).flatMap(_.nonCompliantDetails)
          maybeNonCompliantDetailsList match {
            case Nil => None
            case nonCompliantDetailsList =>
              Some(nonCompliantDetailsList.maxBy(
                nonCompliantDetails => nonCompliantDetails.nonCompliantReturns.getOrElse(0) + nonCompliantDetails.nonCompliantPayments.getOrElse(0)
              ))
          }
        }

        val etmpPreviousEuRegistrationDetails: List[EtmpPreviousEuRegistrationDetails] = {
          for {
            previousEuRegistration <- previousEuRegistrations
            previousScheme <- previousEuRegistration.previousSchemesDetails
          } yield {
            EtmpPreviousEuRegistrationDetails(
              issuedBy = previousEuRegistration.previousEuCountry.code,
              registrationNumber = previousScheme.previousSchemeNumbers.previousSchemeNumber,
              schemeType = convertSchemeType(previousScheme.previousScheme),
              intermediaryNumber = previousScheme.previousSchemeNumbers.previousIntermediaryNumber
            )
          }
        }

        val etmpEuRegistrationDetails: List[EtmpEuRegistrationDetails] = {
          for {
            euDetails <- euRegistrations
            traderId <-  convertToTraderId(euDetails)
          } yield {
            EtmpEuRegistrationDetails(
              countryOfRegistration = euDetails.euCountry.code,
              traderId = traderId,
              tradingName = euDetails.fixedEstablishmentTradingName.value,
              fixedEstablishmentAddressLine1 = euDetails.fixedEstablishmentAddress.map(_.line1).value,
              fixedEstablishmentAddressLine2 = euDetails.fixedEstablishmentAddress.map(_.line2).value,
              townOrCity = euDetails.fixedEstablishmentAddress.map(_.townOrCity).value,
              regionOrState = euDetails.fixedEstablishmentAddress.map(_.stateOrRegion).value,
              postcode = euDetails.fixedEstablishmentAddress.map(_.postCode).value
            )
          }
        }

        val etmpSchemeDetails = EtmpSchemeDetails(
          commencementDate = LocalDate.now(stubClockAtArbitraryDate).format(eisDateFormatter),
          euRegistrationDetails = etmpEuRegistrationDetails,
          previousEURegistrationDetails = etmpPreviousEuRegistrationDetails,
          websites = convertToEtmpWebsite,
          contactName = genBusinessContactDetails.fullName,
          businessTelephoneNumber = genBusinessContactDetails.telephoneNumber,
          businessEmailId = genBusinessContactDetails.emailAddress,
          nonCompliantReturns = getNonCompliantDetails.flatMap(_.nonCompliantReturns.map(_.toString)),
          nonCompliantPayments = getNonCompliantDetails.flatMap(_.nonCompliantPayments.map(_.toString)),
        )

        val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
          administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionCreate),
          customerIdentification = EtmpCustomerIdentificationNew(EtmpIdType.VRN, vrn.vrn),
          tradingNames = convertToEtmpTradingNames,
          schemeDetails = etmpSchemeDetails,
          bankDetails = convertToEtmpBankDetails
        )

        EtmpRegistrationRequest.buildEtmpRegistrationRequest(userAnswers,
          vrn = vrn,
          commencementDate = LocalDate.now(stubClockAtArbitraryDate),
          appConfig = mockFrontendAppConfig
        ) mustBe etmpRegistrationRequest
      }
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "administration" -> 12345,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )
      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }
  }
}

