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
import connectors.ValidateCoreRegistrationConnector
import models.core.{CoreRegistrationValidationResult, Match, MatchType}
import models.PreviousScheme
import models.requests.AuthenticatedDataRequest
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreRegistrationValidationServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with Matchers with BeforeAndAfterEach {

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    "333333333",
    None,
    "EE",
    Some(2),
    None,
    None,
    None,
    None
  )

  private val coreValidationResponses: CoreRegistrationValidationResult =
    CoreRegistrationValidationResult(
      "333333333",
      None,
      "EE",
      traderFound = true,
      Seq(
        genericMatch
      ))

  private val connector = mock[ValidateCoreRegistrationConnector]

  private val auditService = mock[AuditService]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers)

  implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, None, emptyUserAnswers)

  "coreRegistrationValidationService.searchUkVrn" - {

    "call searchUkVrn for any matchType and return match data" in {

      val vrn = Vrn("333333333")

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchUkVrn(vrn).futureValue.get

      value == genericMatch
    }

    "must return None when no active match found" in {

      val vrn = Vrn("333333333")

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchUkVrn(vrn).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val vrn = Vrn("333333333")

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val response = intercept[Exception](coreRegistrationValidationService.searchUkVrn(vrn).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchEuTaxId" - {

    "call searchEuTaxId with correct Tax reference number and must return match data" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue.get

      value == genericMatch
    }

    "must return None when no match found" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val response = intercept[Exception](coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchEuVrn" - {

    "call searchEuTaxId with correct EU VRN and must return match data" in {

      val euVrn: String = "333333333"
      val countrycode: String = "DE"

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchEuVrn(euVrn, countrycode).futureValue.get

      value == genericMatch
    }

    "must return None when no match found" in {

      val euVrn: String = "333333333"
      val countryCode: String = "DE"

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val euVrn: String = "333333333"
      val countryCode: String = "DE"

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val response = intercept[Exception](coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchScheme" - {

    "call searchScheme with correct ioss number and must return match data" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue.get

      value == genericMatch
    }

    "call searchScheme with correct ioss number with intermediary and must return match data" in {

      val iossNumber: String = "IM333222111"
      val intermediaryNumber: String = "IN555444222"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, Some(intermediaryNumber), countryCode).futureValue.get

      value == genericMatch
    }

    "must return None when no match found" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val response = intercept[Exception](coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.isActiveTrader" - {

    "call isActiveTrader with matchType = FixedEstablishmentActiveNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val response = coreRegistrationValidationService.isActiveTrader(genericMatch)

      response == true
    }

    "call isActiveTrader with matchType = TraderIdActiveNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.TraderIdActiveNETP)
      val response = coreRegistrationValidationService.isActiveTrader(newMatch)

      response == true
    }

    "call isActiveTrader with matchType = OtherMSNETPActiveNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)
      val response = coreRegistrationValidationService.isActiveTrader(newMatch)

      response == true
    }

    "call isActiveTrader with matchType = FixedEstablishmentQuarantinedNETP and must return false" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)
      val response = coreRegistrationValidationService.isActiveTrader(newMatch)

      response == false
    }
  }

  "coreRegistrationValidationService.isExcludedTrader" - {

    "call isExcludedTrader with matchType = FixedEstablishmentQuarantinedNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)
      val response = coreRegistrationValidationService.isQuarantinedTrader(newMatch)

      response == true
    }

    "call isExcludedTrader with matchType = TraderIdQuarantinedNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)
      val response = coreRegistrationValidationService.isQuarantinedTrader(newMatch)

      response == true
    }

    "call isExcludedTrader with matchType = OtherMSNETPQuarantinedNETP and must return true" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)
      val response = coreRegistrationValidationService.isQuarantinedTrader(newMatch)

      response == true
    }

    "call isExcludedTrader with matchType = FixedEstablishmentActiveNETP and must return false" in {

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, auditService)

      val newMatch = genericMatch.copy(matchType = MatchType.FixedEstablishmentActiveNETP)
      val response = coreRegistrationValidationService.isQuarantinedTrader(newMatch)

      response == false
    }
  }
}
