/*
 * Copyright 2026 HM Revenue & Customs
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

package generators

import connectors.SavedUserAnswers
import models.*
import models.amend.PreviousRegistration
import models.domain.ModelHelpers.normaliseSpaces
import models.domain.PreviousSchemeNumbers
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import models.etmp.*
import models.etmp.amend.{EtmpAmendRegistrationChangeLog, EtmpAmendRegistrationChangeLogLegacy}
import models.euDetails.{EuDetails, RegistrationType}
import models.intermediaries.{EtmpCustomerIdentification, EtmpDisplayRegistration, EtmpDisplaySchemeDetails, *}
import models.ossExclusions.{ExclusionReason, OssExcludedTrader}
import models.ossRegistration.*
import models.previousRegistrations.NonCompliantDetails
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{choose, listOfN, option}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.EitherValues
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

trait ModelGenerators extends EitherValues {

  private val maxFieldLength: Int = 35
  private val maxEuTaxReferenceLength: Int = 20

  implicit lazy val arbitraryCheckVatDetails: Arbitrary[CheckVatDetails] =
    Arbitrary {
      Gen.oneOf(CheckVatDetails.values)
    }

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        line3 <- Gen.option(commonFieldString(maxFieldLength))
        line4 <- Gen.option(commonFieldString(maxFieldLength))
        line5 <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries.map(_.code))
      } yield DesAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(line3),
        normaliseSpaces(line4),
        normaliseSpaces(line5),
        normaliseSpaces(postCode),
        country
      )
    }

  implicit lazy val arbitraryInternationalAddress: Arbitrary[InternationalAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        townOrCity <- commonFieldString(maxFieldLength)
        stateOrRegion <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries)
      } yield InternationalAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(townOrCity),
        normaliseSpaces(stateOrRegion),
        normaliseSpaces(postCode),
        country
      )
    }

  implicit lazy val arbitraryTradingName: Arbitrary[TradingName] =
    Arbitrary {
      for {
        name <- commonFieldString(maxFieldLength)
      } yield {
        TradingName(name)
      }
    }

  implicit lazy val arbitraryWebsite: Arbitrary[Website] =
    Arbitrary {
      for {
        site <- Gen.alphaStr
      } yield Website(site)
    }

  private def commonFieldString(maxLength: Int): Gen[String] = (for {
    length <- choose(1, maxLength)
    chars <- listOfN(length, commonFieldSafeInputs)
  } yield chars.mkString).retryUntil(_.trim.nonEmpty)

  private def commonFieldSafeInputs: Gen[Char] = Gen.oneOf(
    Gen.alphaNumChar,
    Gen.oneOf('À' to 'ÿ'),
    Gen.const('.'),
    Gen.const(','),
    Gen.const('/'),
    Gen.const('’'),
    Gen.const('\''),
    Gen.const('"'),
    Gen.const('_'),
    Gen.const('&'),
    Gen.const(' '),
    Gen.const('\'')
  )

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit lazy val arbitraryRegistrationType: Arbitrary[RegistrationType] =
    Arbitrary {
      Gen.oneOf(RegistrationType.values)
    }

  implicit lazy val arbitraryEuTaxReference: Gen[String] = {
    Gen.listOfN(maxEuTaxReferenceLength, Gen.alphaNumChar).map(_.mkString)
  }

  implicit lazy val arbitraryEuVatNumber: Gen[String] =
    for {
      countryCode <- Gen.oneOf(Country.euCountries.map(_.code))
      matchedCountryRule = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode).head
    } yield s"$countryCode${matchedCountryRule.exampleVrn}"

  implicit lazy val arbitraryEuDetails: Arbitrary[EuDetails] =
    Arbitrary {
      for {
        country <- arbitraryCountry.arbitrary
        sellsGoodsToEUConsumerMethod <- Gen.option(arbitrary[Boolean])
        registrationType <- Gen.option(arbitraryRegistrationType.arbitrary)
        euVatNumber <- Gen.option(arbitraryEuVatNumber)
        euTaxReference <- Gen.option(arbitraryEuTaxReference)
        fixedEstablishmentTradingName <- Gen.option(arbitrary[String])
        fixedEstablishmentAddress <- Gen.option(arbitraryInternationalAddress.arbitrary)
      } yield EuDetails(
        country,
        sellsGoodsToEUConsumerMethod,
        registrationType,
        euVatNumber,
        euTaxReference,
        fixedEstablishmentTradingName,
        fixedEstablishmentAddress
      )
    }

  implicit lazy val arbitraryBusinessContactDetails: Arbitrary[BusinessContactDetails] =
    Arbitrary {
      for {
        fullName <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress <- arbitrary[String]
      } yield BusinessContactDetails(fullName, telephoneNumber, emailAddress)
    }

  def ukPostcode(): Gen[String] =
    for {
      numberOfFirstLetters <- choose(1, 2)
      firstLetters <- listOfN(numberOfFirstLetters, Gen.alphaChar)
      firstNumber <- Gen.numChar
      numberOfMiddle <- choose(0, 1)
      middle <- listOfN(numberOfMiddle, Gen.alphaNumChar)
      lastNumber <- Gen.numChar
      lastLetters <- listOfN(2, Gen.alphaChar)
    } yield firstLetters.mkString + firstNumber.toString + middle.mkString + lastNumber.toString + lastLetters.mkString


  implicit lazy val arbitraryPreviousScheme: Arbitrary[PreviousScheme] =
    Arbitrary {
      Gen.oneOf(PreviousScheme.values)
    }

  implicit lazy val arbitraryPreviousSchemeType: Arbitrary[PreviousSchemeType] =
    Arbitrary {
      Gen.oneOf(PreviousSchemeType.values)
    }

  implicit lazy val arbitraryPreviousIossSchemeDetails: Arbitrary[PreviousSchemeNumbers] =
    Arbitrary {
      PreviousSchemeNumbers("12345667", Some("test"))
    }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }

  implicit lazy val arbitraryBankDetails: Arbitrary[BankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield BankDetails(accountName, bic, iban)
    }

  implicit lazy val arbitraryIban: Arbitrary[Iban] =
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).getOrElse(throw new IllegalArgumentException(s"Invalid IBAN: $v")))
    }

  implicit val arbitrarySavedUserAnswers: Arbitrary[SavedUserAnswers] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        data = JsObject(Seq("test" -> Json.toJson("test")))
        now = Instant.now
      } yield SavedUserAnswers(vrn, data, None, now)
    }

  implicit lazy val arbitraryVrn: Arbitrary[Vrn] =
    Arbitrary {
      for {
        chars <- Gen.listOfN(9, Gen.numChar)
      } yield Vrn(chars.mkString(""))
    }

  implicit lazy val arbitraryEtmpAdministration: Arbitrary[EtmpAdministration] =
    Arbitrary {
      for {
        messageType <- Gen.oneOf(EtmpMessageType.values)
      } yield EtmpAdministration(messageType, "IOSS")
    }

  implicit lazy val arbitraryEtmpCustomerIdentificationNew: Arbitrary[EtmpCustomerIdentificationNew] =
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
      } yield EtmpCustomerIdentificationNew(EtmpIdType.VRN, vrn.vrn)
    }

  implicit lazy val arbitraryEtmpCustomerIdentificationLegacy: Arbitrary[EtmpCustomerIdentificationLegacy] =
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
      } yield EtmpCustomerIdentificationLegacy(vrn)
    }

  implicit lazy val arbitraryVatNumberTraderId: Arbitrary[VatNumberTraderId] =
    Arbitrary {
      for {
        vatNumber <- Gen.alphaNumStr
      } yield VatNumberTraderId(vatNumber)
    }

  implicit lazy val arbitraryTaxRefTraderID: Arbitrary[TaxRefTraderID] =
    Arbitrary {
      for {
        taxReferenceNumber <- Gen.alphaNumStr
      } yield TaxRefTraderID(taxReferenceNumber)
    }

  implicit lazy val arbitraryEtmpWebsite: Arbitrary[EtmpWebsite] =
    Arbitrary {
      for {
        websiteAddress <- Gen.alphaStr
      } yield EtmpWebsite(websiteAddress)
    }

  implicit lazy val arbitraryEtmpTradingName: Arbitrary[EtmpTradingName] =
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield EtmpTradingName(tradingName)
    }

  implicit lazy val arbitrarySchemeType: Arbitrary[SchemeType] =
    Arbitrary {
      Gen.oneOf(SchemeType.values)
    }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  implicit lazy val arbitraryDate: Arbitrary[LocalDate] =
    Arbitrary {
      datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2023, 12, 31))
    }

  implicit lazy val arbitraryEtmpExclusionReason: Arbitrary[EtmpExclusionReason] =
    Arbitrary {
      Gen.oneOf(EtmpExclusionReason.values)
    }

  implicit lazy val arbitraryEtmpAmendRegistrationChangeLog: Arbitrary[EtmpAmendRegistrationChangeLog] =
    Arbitrary {
      for {
        tradingNames <- arbitrary[Boolean]
        fixedEstablishments <- arbitrary[Boolean]
        contactDetails <- arbitrary[Boolean]
        bankDetails <- arbitrary[Boolean]
        reRegistration <- arbitrary[Boolean]
      } yield EtmpAmendRegistrationChangeLogLegacy(tradingNames, fixedEstablishments, contactDetails, bankDetails, reRegistration)
    }

  implicit lazy val arbitraryEtmpExclusion: Arbitrary[EtmpExclusion] =
    Arbitrary {
      for {
        exclusionReason <- arbitraryEtmpExclusionReason.arbitrary
        effectiveDate <- arbitraryDate.arbitrary
        decisionDateDate <- arbitraryDate.arbitrary
        quarantine <- arbitrary[Boolean]
      } yield EtmpExclusion(
        exclusionReason = exclusionReason,
        effectiveDate = effectiveDate,
        decisionDate = decisionDateDate,
        quarantine = quarantine
      )
    }

  implicit lazy val arbitraryNonCompliantDetails: Arbitrary[NonCompliantDetails] =
    Arbitrary {
      for {
        nonCompliantReturns <- option(Gen.chooseNum(1, 2))
        nonCompliantPayments <- option(Gen.chooseNum(1, 2))
      } yield {
        NonCompliantDetails(
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )
      }
    }

  implicit val arbitraryEACDIdentifiers: Arbitrary[EACDIdentifiers] = {
    Arbitrary {
      for {
        key <- Gen.alphaStr
        value <- Gen.alphaStr
      } yield EACDIdentifiers(
        key = key,
        value = value
      )
    }
  }

  implicit val arbitraryEACDEnrolment: Arbitrary[EACDEnrolment] = {
    Arbitrary {
      for {
        service <- Gen.alphaStr
        state <- Gen.alphaStr
        identifiers <- Gen.listOfN(2, arbitraryEACDIdentifiers.arbitrary)
      } yield EACDEnrolment(
        service = service,
        state = state,
        activationDate = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)),
        identifiers = identifiers
      )
    }
  }

  implicit val arbitraryEACDEnrolments: Arbitrary[EACDEnrolments] = {
    Arbitrary {
      for {
        enrolments <- Gen.listOfN(2, arbitraryEACDEnrolment.arbitrary)
      } yield EACDEnrolments(
        enrolments = enrolments
      )
    }
  }

  implicit val arbitraryPreviousRegistration: Arbitrary[PreviousRegistration] = {
    Arbitrary {
      for {
        iossNumber <- Gen.alphaStr
        startPeriod <- arbitraryDate.arbitrary
        endPeriod <- arbitraryDate.arbitrary
      } yield PreviousRegistration(
        iossNumber = iossNumber,
        startPeriod = startPeriod,
        endPeriod = endPeriod
      )
    }
  }

  implicit val arbitraryOssExcludedTrader: Arbitrary[OssExcludedTrader] = {
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
        exclusionReason <- Gen.oneOf(ExclusionReason.values)
        effectiveDate <- arbitraryDate.arbitrary
        quarantined <- arbitrary[Boolean]
      } yield OssExcludedTrader(
        vrn = vrn,
        exclusionReason = Some(exclusionReason),
        effectiveDate = Some(effectiveDate),
        quarantined = Some(quarantined)
      )
    }
  }

  implicit val arbitraryOssRegistration: Arbitrary[OssRegistration] = {
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
        name <- arbitrary[String]
        vatDetails <- arbitrary[OssVatDetails]
        contactDetails <- arbitrary[OssContactDetails]
        bankDetails <- arbitrary[BankDetails]
        commencementDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        isOnlineMarketplace <- arbitrary[Boolean]
        adminUse <- arbitrary[OssAdminUse]
      } yield OssRegistration(vrn, name, Nil, vatDetails, Nil, contactDetails, Nil, commencementDate, Nil, bankDetails, isOnlineMarketplace, None, None, None, None, None, None, None, None, adminUse)
    }
  }

  implicit lazy val arbitraryOssVatDetails: Arbitrary[OssVatDetails] =
    Arbitrary {
      for {
        registrationDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        address <- arbitrary[Address]
        partOfVatGroup <- arbitrary[Boolean]
        source <- arbitrary[OssVatDetailSource]
      } yield OssVatDetails(registrationDate, address, partOfVatGroup, source)
    }

  implicit val arbitraryOssVatDetailSource: Arbitrary[OssVatDetailSource] =
    Arbitrary(
      Gen.oneOf(OssVatDetailSource.values)
    )

  implicit lazy val arbitraryOssBusinessContactDetails: Arbitrary[OssContactDetails] =
    Arbitrary {
      for {
        fullName <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress <- arbitrary[String]
      } yield OssContactDetails(fullName, telephoneNumber, emailAddress)
    }

  implicit lazy val arbitraryAdminUse: Arbitrary[OssAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield OssAdminUse(Some(changeDate))
    }

  implicit val arbitraryAddress: Arbitrary[Address] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[InternationalAddress],
        arbitrary[DesAddress]
      )
    }

  implicit lazy val arbitrarySalesChannels: Arbitrary[SalesChannels] =
    Arbitrary {
      Gen.oneOf(SalesChannels.values)
    }

  implicit lazy val arbitraryFixedEstablishment: Arbitrary[OssTradeDetails] =
    Arbitrary {
      for {
        tradingName <- arbitrary[String]
        address <- arbitrary[InternationalAddress]
      } yield OssTradeDetails(tradingName, address)
    }

  implicit val arbitraryEuTaxIdentifierType: Arbitrary[OssEuTaxIdentifierType] =
    Arbitrary {
      Gen.oneOf(OssEuTaxIdentifierType.values)
    }

  implicit val arbitraryEuTaxIdentifier: Arbitrary[OssEuTaxIdentifier] =
    Arbitrary {
      for {
        identifierType <- arbitrary[OssEuTaxIdentifierType]
        value <- arbitrary[Int].map(_.toString)
      } yield OssEuTaxIdentifier(identifierType, value)
    }

  implicit lazy val arbitraryIossNumber: Arbitrary[String] =
    Arbitrary {
      for {
        iossNumber <- Gen.listOfN(7, Gen.numChar).map(_.mkString)
      } yield {
        s"IM900$iossNumber"
      }
    }

  implicit lazy val arbitraryEtmpClientDetails: Arbitrary[EtmpClientDetails] =
    Arbitrary {
      for {
        clientName <- Gen.alphaStr
        clientIossID <- arbitraryIossNumber.arbitrary
        clientExcluded <- arbitrary[Boolean]
      } yield {
        EtmpClientDetails(
          clientName = clientName,
          clientIossID = clientIossID,
          clientExcluded = clientExcluded
        )
      }
    }

  implicit lazy val arbitraryEtmpCustomerIdentification: Arbitrary[EtmpCustomerIdentification] =
    Arbitrary {
      for {
        idType <- Gen.oneOf(EtmpIdType.values)
        vrn <- arbitraryVrn.arbitrary
      } yield EtmpCustomerIdentification(idType, vrn.vrn)
    }

  implicit lazy val genEuTaxReference: Gen[String] =
    Gen.listOfN(20, Gen.alphaNumChar).map(_.mkString)

  implicit lazy val genIntermediaryNumber: Gen[String] =
    for {
      intermediaryNumber <- Gen.listOfN(12, Gen.alphaChar).map(_.mkString)
    } yield intermediaryNumber

  implicit lazy val arbitraryOtherIossIntermediaryRegistrations: Arbitrary[EtmpOtherIossIntermediaryRegistrations] =
    Arbitrary {
      for {
        issuedBy <- arbitraryCountry.arbitrary.map(_.code)
        intermediaryNumber <- genIntermediaryNumber
      } yield {
        EtmpOtherIossIntermediaryRegistrations(
          issuedBy = issuedBy,
          intermediaryNumber = intermediaryNumber
        )
      }
    }

  implicit lazy val arbitraryIntermediaryDetails: Arbitrary[EtmpIntermediaryDetails] =
    Arbitrary {
      for {
        otherIossIntermediaryRegistrations <- Gen.listOfN(2, arbitraryOtherIossIntermediaryRegistrations.arbitrary)
      } yield {
        EtmpIntermediaryDetails(
          otherIossIntermediaryRegistrations = otherIossIntermediaryRegistrations
        )
      }
    }

  implicit lazy val arbitraryEtmpOtherAddress: Arbitrary[EtmpOtherAddress] =
    Arbitrary {
      for {
        issuedBy <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
        tradingName <- Gen.listOfN(20, Gen.alphaChar).map(_.mkString)
        addressLine1 <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        addressLine2 <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        townOrCity <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        regionOrState <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        postcode <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
      } yield EtmpOtherAddress(
        issuedBy,
        Some(tradingName),
        addressLine1,
        Some(addressLine2),
        townOrCity,
        Some(regionOrState),
        Some(postcode)
      )
    }

  implicit lazy val arbitraryEtmpDisplayEuRegistrationDetails: Arbitrary[EtmpDisplayEuRegistrationDetails] =
    Arbitrary {
      for {
        issuedBy <- arbitraryCountry.arbitrary.map(_.code)
        vatNumber <- arbitraryEuVatNumber
        taxIdentificationNumber <- genEuTaxReference
        fixedEstablishmentTradingName <- arbitraryEtmpTradingName.arbitrary.map(_.tradingName)
        fixedEstablishmentAddressLine1 <- Gen.alphaStr
        fixedEstablishmentAddressLine2 <- Gen.alphaStr
        townOrCity <- Gen.alphaStr
        regionOrState <- Gen.alphaStr
        postcode <- Gen.alphaStr
      } yield {
        EtmpDisplayEuRegistrationDetails(
          issuedBy = issuedBy,
          vatNumber = Some(vatNumber),
          taxIdentificationNumber = Some(taxIdentificationNumber),
          fixedEstablishmentTradingName = fixedEstablishmentTradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = Some(fixedEstablishmentAddressLine2),
          townOrCity = townOrCity,
          regionOrState = Some(regionOrState),
          postcode = Some(postcode)
        )
      }
    }

  implicit lazy val arbitraryEtmpDisplaySchemeDetails: Arbitrary[EtmpDisplaySchemeDetails] =
    Arbitrary {
      for {
        commencementDate <- arbitrary[LocalDate].map(_.toString)
        euRegistrationDetails <- Gen.listOfN(3, arbitraryEtmpDisplayEuRegistrationDetails.arbitrary)
        contactName <- Gen.alphaStr
        businessTelephoneNumber <- Gen.alphaNumStr
        businessEmailId <- Gen.alphaStr
        unusableStatus <- arbitrary[Boolean]
        nonCompliant <- Gen.oneOf("1", "2")
      } yield {
        EtmpDisplaySchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          unusableStatus = unusableStatus,
          nonCompliantReturns = Some(nonCompliant),
          nonCompliantPayments = Some(nonCompliant)
        )
      }
    }

  implicit lazy val arbitraryEtmpBankDetails: Arbitrary[EtmpBankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- arbitraryBic.arbitrary
        iban <- arbitraryIban.arbitrary
      } yield {
        EtmpBankDetails(
          accountName = accountName,
          bic = Some(bic),
          iban = iban
        )
      }
    }

  implicit lazy val arbitraryEtmpAdminUse: Arbitrary[EtmpAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield EtmpAdminUse(changeDate = Some(changeDate))
    }

  implicit lazy val arbitraryEtmpDisplayRegistration: Arbitrary[EtmpDisplayRegistration] =
    Arbitrary {
      for {
        customerIdentification <- arbitraryEtmpCustomerIdentification.arbitrary
        tradingNames <- Gen.listOfN(3, arbitraryEtmpTradingName.arbitrary)
        clientDetails <- Gen.listOfN(3, arbitraryEtmpClientDetails.arbitrary)
        intermediaryDetails <- arbitraryIntermediaryDetails.arbitrary
        otherAddress <- arbitraryEtmpOtherAddress.arbitrary
        schemeDetails <- arbitraryEtmpDisplaySchemeDetails.arbitrary
        exclusions <- Gen.listOfN(1, arbitraryEtmpExclusion.arbitrary)
        bankDetails <- arbitraryEtmpBankDetails.arbitrary
        adminUse <- arbitraryEtmpAdminUse.arbitrary
      } yield {
        EtmpDisplayRegistration(
          customerIdentification = customerIdentification,
          tradingNames = tradingNames,
          clientDetails = clientDetails,
          intermediaryDetails = Some(intermediaryDetails),
          otherAddress = Some(otherAddress),
          schemeDetails = schemeDetails,
          exclusions = exclusions,
          bankDetails = bankDetails,
          adminUse = adminUse
        )
      }
    }
}


