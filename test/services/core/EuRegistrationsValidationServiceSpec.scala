/*
 * Copyright 2024 HM Revenue & Customs
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

package services.core

import base.SpecBase
import models.PreviousScheme.{IOSSWI, IOSSWOI, OSSNU, OSSU}
import models.core.MatchType.{FixedEstablishmentQuarantinedNETP, TransferringMSID}
import models.core.{Match, MatchType}
import models.etmp.SchemeType.{IOSSWithIntermediary, IOSSWithoutIntermediary, OSSNonUnion, OSSUnion}
import models.etmp.{EtmpDisplayEuRegistrationDetails, EtmpPreviousEuRegistrationDetails, SchemeType}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, IdiomaticMockito, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EuRegistrationsValidationServiceSpec
  extends SpecBase
    with IdiomaticMockito
    with ScalaFutures
    with Matchers
    with BeforeAndAfterEach
    with TableDrivenPropertyChecks {

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]
  private val previousRegistrationValidationService = new EuRegistrationsValidationService(mockCoreRegistrationValidationService)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers, None)
  implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, None, emptyUserAnswers, None)

  private val quarantinedNETP = FixedEstablishmentQuarantinedNETP
  private val activeNETP = MatchType.FixedEstablishmentActiveNETP

  override protected def beforeEach(): Unit =
    reset(mockCoreRegistrationValidationService)

  "validateEuRegistrationDetails must" - {
    "return success for an empty list" in {
      previousRegistrationValidationService.validateEuRegistrationDetails(List.empty).futureValue mustBe Right(true)
    }

    "return InvalidActiveTraderResult when something is found on a VRN that is active" in {
      val vrn = "VRN1"
      val countryCode = "IE"
      val details = List(
        createEtmpDisplayEuRegistrationDetails(countryCode, Some(vrn), None)
      )

      val foundMatch = createMatch(activeNETP)
      when(mockCoreRegistrationValidationService.searchEuVrn(
        euVrn = ArgumentMatchers.eq(vrn),
        countryCode = ArgumentMatchers.eq(countryCode)
      )(any(), any()))
        .thenReturn(Future.successful(Some(foundMatch)))

      previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe Left(InvalidActiveTrader(countryCode, foundMatch.memberState))
    }

    def createEtmpDisplayEuRegistrationDetails(countryCode: String, maybeVrn: Option[String], maybeTaxIdentifierNumber: Option[String]) =
      EtmpDisplayEuRegistrationDetails(
        issuedBy = countryCode,
        vatNumber = maybeVrn,
        taxIdentificationNumber = maybeTaxIdentifierNumber,
        fixedEstablishmentTradingName = "",
        fixedEstablishmentAddressLine1 = "",
        fixedEstablishmentAddressLine2 = None,
        townOrCity = "",
        regionOrState = None,
        postcode = None
      )

    "return InvalidQuarantinedTraderResult when something is found on a VRN that is active" in {
      val vrn = "VRN1"
      val countryCode = "IE"
      val details = List(
        createEtmpDisplayEuRegistrationDetails(countryCode, Some(vrn), None)
      )

      when(mockCoreRegistrationValidationService.searchEuVrn(
        euVrn = ArgumentMatchers.eq(vrn),
        countryCode = ArgumentMatchers.eq(countryCode)
      )(any(), any()))
        .thenReturn(Future.successful(Some(createMatch(FixedEstablishmentQuarantinedNETP))))

      previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe
        Left(InvalidQuarantinedTrader)
    }

    "return InvalidActiveTraderResult when an active result is found on a Tax Identifier" in {
      val countryCode = "SI"
      val euTaxReference = "ID1"
      val details = List(
        createEtmpDisplayEuRegistrationDetails(countryCode, None, Some(euTaxReference)),
      )

      val foundMatch = createMatch(activeNETP)
      when(mockCoreRegistrationValidationService.searchEuTaxId(
        euTaxReference = ArgumentMatchers.eq(euTaxReference),
        countryCode = ArgumentMatchers.eq(countryCode))(any(), any())
      )
        .thenReturn(Future.successful(Some(foundMatch)))

      previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe
        Left(InvalidActiveTrader(countryCode, foundMatch.memberState))
    }

    "return InvalidQuarantinedTraderResult when a quarantined result is found on a Tax Identifier" in {
      val countryCode = "SI"
      val euTaxReference = "ID1"
      val details = List(
        createEtmpDisplayEuRegistrationDetails(countryCode, None, Some(euTaxReference)),
      )

      when(mockCoreRegistrationValidationService.searchEuTaxId(
        euTaxReference = ArgumentMatchers.eq(euTaxReference),
        countryCode = ArgumentMatchers.eq(countryCode))(any(), any())
      )
        .thenReturn(Future.successful(Some(createMatch(quarantinedNETP))))

      previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe
        Left(InvalidQuarantinedTrader)
    }

    "fail on first failure" in {
      val countryCode1 = "SI"
      val countryCode2 = "RO"
      val vrn1 = "VRN1"
      val vrn2 = "VRN2"
      val euTaxReference1 = "ID1"
      val euTaxReference2 = "ID2"

      val details = List(
        createEtmpDisplayEuRegistrationDetails(countryCode1, None, Some(euTaxReference1)),
        createEtmpDisplayEuRegistrationDetails(countryCode2, Some(vrn1), None),
        createEtmpDisplayEuRegistrationDetails(countryCode1, Some(vrn2), None),
        createEtmpDisplayEuRegistrationDetails(countryCode2, None, Some(euTaxReference2)),
      )

      when(mockCoreRegistrationValidationService.searchEuTaxId(
        euTaxReference = ArgumentMatchers.eq(euTaxReference1),
        countryCode = ArgumentMatchers.eq(countryCode1))(any(), any())
      )
        .thenReturn(Future.successful(Some(createMatch(TransferringMSID))))

      when(mockCoreRegistrationValidationService.searchEuVrn(
        euVrn = ArgumentMatchers.eq(vrn1),
        countryCode = ArgumentMatchers.eq(countryCode2)
      )(any(), any()))
        .thenReturn(Future.successful(Some(createMatch(quarantinedNETP))))

      when(mockCoreRegistrationValidationService.searchEuVrn(
        euVrn = ArgumentMatchers.eq(vrn2),
        countryCode = ArgumentMatchers.eq(countryCode1)
      )(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockCoreRegistrationValidationService.searchEuTaxId(
        euTaxReference = ArgumentMatchers.eq(euTaxReference2),
        countryCode = ArgumentMatchers.eq(countryCode2))(any(), any())
      )
        .thenReturn(Future.successful(Some(createMatch(activeNETP))))


      previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe
        Left(InvalidQuarantinedTrader)
    }


    "return success when nothing active or quarantined is found on a VRN" in {
      val options = Table(
        "validation response",
        None,
        Some(createMatch(TransferringMSID))
      )

      forAll(options) { result =>
        reset(mockCoreRegistrationValidationService)
        val vrn = "VRN1"
        val countryCode = "IE"
        val details = List(
          createEtmpDisplayEuRegistrationDetails(countryCode, Some(vrn), None)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(
          euVrn = ArgumentMatchers.eq(vrn),
          countryCode = ArgumentMatchers.eq(countryCode)
        )(any(), any()))
          .thenReturn(Future.successful(result))

        previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe Right(true)
      }
    }

    "return success when nothing active or quarantined is found on a Tax id" in {
      val options = Table(
        "validation response",
        None,
        Some(createMatch(TransferringMSID))
      )

      forAll(options) { result =>
        reset(mockCoreRegistrationValidationService)
        val euTaxReference = "ID1"
        val countryCode = "IE"
        val details = List(
          createEtmpDisplayEuRegistrationDetails(countryCode, None, Some(euTaxReference))
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(
          euTaxReference = ArgumentMatchers.eq(euTaxReference),
          countryCode = ArgumentMatchers.eq(countryCode))(any(), any())
        )
          .thenReturn(Future.successful(result))

        previousRegistrationValidationService.validateEuRegistrationDetails(details).futureValue mustBe Right(true)
      }
    }
  }

  def createMatch(matchType: MatchType): Match = Match(
    matchType = matchType,
    traderId = "",
    intermediary = None,
    memberState = "",
    exclusionStatusCode = None,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = None,
    nonCompliantReturns = None,
    nonCompliantPayments = None
  )


  "validatePreviousEuRegistrationDetails must" - {
    "return success when no infractions an empty list is passed" in {
      previousRegistrationValidationService.validatePreviousEuRegistrationDetails(List.empty).futureValue mustBe Right(true)
    }

    "return success when nothing is found for all but OSSNonUnion which is ignored" in {
      val registrationNumber = "registrationNumber"
      val maybeIntermediaryNumber = Some("inumber1")
      val countryCode = "IE"

      val nonOSSUnionOptions = Table(
        ("non OSSNonUnion option", "mapped non OSSNU Option"),
        (OSSUnion, OSSU),
        (IOSSWithoutIntermediary, IOSSWOI),
        (IOSSWithIntermediary, IOSSWI)
      )

      forAll(nonOSSUnionOptions) { (schemaType, previousScheme) =>
        when(
          mockCoreRegistrationValidationService.searchScheme(registrationNumber, previousScheme, maybeIntermediaryNumber, countryCode)
        ).thenReturn(Future.successful(None))

        previousRegistrationValidationService.validatePreviousEuRegistrationDetails(
          List(createEtmpPreviousEuRegistrationDetails(countryCode, registrationNumber, schemaType, maybeIntermediaryNumber))
        ).futureValue mustBe Right(true)
      }
    }

    def createEtmpPreviousEuRegistrationDetails(countryCode: String,
                                                registrationNumber: String,
                                                schemaType: SchemeType,
                                                intermediaryNumber: Option[String]): EtmpPreviousEuRegistrationDetails = {
      EtmpPreviousEuRegistrationDetails(
        issuedBy = countryCode,
        registrationNumber = registrationNumber,
        schemeType = schemaType,
        intermediaryNumber = intermediaryNumber
      )
    }

    "just ignore OSS SchemaTypes if not quarantined" in {
      val registrationNumber = "registrationNumber"
      val maybeIntermediaryNumber = Some("inumber1")
      val countryCode = "IE"

      val ossSchemaTypeOptions = Table(
        ("OSS SchemaType", "remapped schema type"),
        (OSSNonUnion, OSSNU),
        (OSSUnion, OSSU)
      )

      forAll(ossSchemaTypeOptions) { (ossSchemaType, expectedRemappedType) =>
        Mockito.reset(mockCoreRegistrationValidationService)
        when(mockCoreRegistrationValidationService.searchScheme(
          ArgumentMatchers.eq(registrationNumber),
          ArgumentMatchers.eq(expectedRemappedType),
          ArgumentMatchers.eq(maybeIntermediaryNumber),
          ArgumentMatchers.eq(countryCode)
        )(any(), any()))
          .thenReturn(Future.successful(Some(createMatch(activeNETP))))

        previousRegistrationValidationService.validatePreviousEuRegistrationDetails(
          List(createEtmpPreviousEuRegistrationDetails(countryCode, registrationNumber, ossSchemaType, maybeIntermediaryNumber))
        ).futureValue mustBe Right(true)
      }
    }

    "not ignore OSS SchemaTypes if quarantined" in {
      val registrationNumber = "registrationNumber"
      val maybeIntermediaryNumber = Some("inumber1")
      val countryCode = "IE"

      val ossSchemaTypeOptions = Table(
        ("OSS SchemaType", "remapped schema type"),
        (OSSNonUnion, OSSNU),
        (OSSUnion, OSSU)
      )

      forAll(ossSchemaTypeOptions) { (ossSchemaType, expectedRemappedType) =>
        Mockito.reset(mockCoreRegistrationValidationService)
        when(mockCoreRegistrationValidationService.searchScheme(
          ArgumentMatchers.eq(registrationNumber),
          ArgumentMatchers.eq(expectedRemappedType),
          ArgumentMatchers.eq(maybeIntermediaryNumber),
          ArgumentMatchers.eq(countryCode)
        )(any(), any()))
          .thenReturn(Future.successful(Some(createMatch(quarantinedNETP))))

        previousRegistrationValidationService.validatePreviousEuRegistrationDetails(
          List(createEtmpPreviousEuRegistrationDetails(countryCode, registrationNumber, ossSchemaType, maybeIntermediaryNumber))
        ).futureValue mustBe Left(InvalidQuarantinedTrader)
      }
    }

    "return InvalidQuarantinedTraderResult when the match is quarantined" in {
      val registrationNumber = "registrationNumber"
      val maybeIntermediaryNumber = Some("inumber1")
      val countryCode = "IE"

      when(
        mockCoreRegistrationValidationService.searchScheme(registrationNumber, IOSSWI, maybeIntermediaryNumber, countryCode)
      ).thenReturn(Future.successful(Some(createMatch(FixedEstablishmentQuarantinedNETP))))

      previousRegistrationValidationService.validatePreviousEuRegistrationDetails(
        List(createEtmpPreviousEuRegistrationDetails(countryCode, registrationNumber, IOSSWithIntermediary, maybeIntermediaryNumber))
      ).futureValue mustBe Left(InvalidQuarantinedTrader)
    }

    "return InvalidActiveTraderResult when the match is active" in {
      val registrationNumber = "registrationNumber"
      val maybeIntermediaryNumber = Some("inumber1")
      val countryCode = "IE"

      val foundMatch = createMatch(activeNETP)
      when(
        mockCoreRegistrationValidationService.searchScheme(registrationNumber, IOSSWI, maybeIntermediaryNumber, countryCode)
      ).thenReturn(Future.successful(Some(foundMatch)))

      previousRegistrationValidationService.validatePreviousEuRegistrationDetails(
        List(createEtmpPreviousEuRegistrationDetails(countryCode, registrationNumber, IOSSWithIntermediary, maybeIntermediaryNumber))
      ).futureValue mustBe Left(InvalidActiveTrader(countryCode, foundMatch.memberState))
    }

    "fail on first error" in {
      val countryCode1 = "SI"
      val countryCode2 = "RO"
      val intermediaryNumber1 = "inumber1"
      val intermediaryNumber2 = "inumber2"
      val intermediaryNumber3 = "inumber3"
      val intermediaryNumber4 = "inumber4"
      val details = List(
        createEtmpPreviousEuRegistrationDetails(countryCode1, "reg1", OSSNonUnion, Some(intermediaryNumber1)),
        createEtmpPreviousEuRegistrationDetails(countryCode2, "reg2", OSSUnion, Some(intermediaryNumber2)),
        createEtmpPreviousEuRegistrationDetails(countryCode1, "reg3", IOSSWithoutIntermediary, Some(intermediaryNumber3)),
        createEtmpPreviousEuRegistrationDetails(countryCode2, "reg4", IOSSWithIntermediary, Some(intermediaryNumber4))
      )

      when(
        mockCoreRegistrationValidationService.searchScheme("reg1", OSSNU, Some(intermediaryNumber1), countryCode1)
      ).thenReturn(Future.successful(Some(createMatch(activeNETP))))

      when(
        mockCoreRegistrationValidationService.searchScheme("reg2", OSSU, Some(intermediaryNumber2), countryCode2)
      ).thenReturn(Future.successful(Some(createMatch(quarantinedNETP))))

      when(
        mockCoreRegistrationValidationService.searchScheme("reg3", IOSSWI, Some(intermediaryNumber3), countryCode1)
      ).thenReturn(Future.successful(Some(createMatch(TransferringMSID))))


      previousRegistrationValidationService.validatePreviousEuRegistrationDetails(details).futureValue mustBe Left(InvalidQuarantinedTrader)
    }

  }
}
