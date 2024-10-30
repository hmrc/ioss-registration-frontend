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

package services.oss

import base.SpecBase
import connectors.RegistrationConnector
import models.ossExclusions.{ExclusionReason, OssExcludedTrader}
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.when
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class OssExclusionsServiceSpec extends SpecBase with PrivateMethodTester with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val currentDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

  private val arbOssExcludedTrader: OssExcludedTrader = arbitraryOssExcludedTrader.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "OssExclusionsService" - {

    ".determineOssExclusionStatus" - {

      "must return true when an OSS excluded trader is both quarantined and the exclusion reason is code 4 and the effective date is within 2 years" in {

        val ossExcludedTrader = arbOssExcludedTrader.copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = Some(currentDate.minusYears(2)),
          quarantined = Some(true)
        )

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(ossExcludedTrader).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.determineOssExclusionStatus(ossExcludedTrader.vrn.vrn).futureValue

        result mustBe true
      }

      "must return false when an OSS excluded trader is quarantined and the exclusion reason is not code 4 and the effective date is within 2 years" in {

        val updatedOssExcludedTrader: OssExcludedTrader = arbOssExcludedTrader
          .copy(exclusionReason = Some(Gen.oneOf(ExclusionReason.values).retryUntil(_ != ExclusionReason.FailsToComply).sample.value))

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(updatedOssExcludedTrader).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.determineOssExclusionStatus(arbOssExcludedTrader.vrn.vrn).futureValue

        result mustBe false
      }

      "must return false when an OSS excluded trader is not quarantined and the exclusion reason is code 4 and the effective date is within 2 years" in {

        val updatedOssExcludedTrader: OssExcludedTrader = arbOssExcludedTrader.copy(quarantined = Some(false))

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(updatedOssExcludedTrader).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.determineOssExclusionStatus(arbOssExcludedTrader.vrn.vrn).futureValue

        result mustBe false
      }

      "must return false when an OSS excluded trader is not quarantined and the exclusion reason is not code 4 and the effective date is outside 2 years" in {

        val updatedOssExcludedTrader: OssExcludedTrader = arbOssExcludedTrader
          .copy(
            quarantined = Some(false),
            exclusionReason = Some(Gen.oneOf(ExclusionReason.values).retryUntil(_ != ExclusionReason.FailsToComply).sample.value),
            effectiveDate = Some(currentDate.minusYears(2).minusDays(1))
          )

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(updatedOssExcludedTrader).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.determineOssExclusionStatus(arbOssExcludedTrader.vrn.vrn).futureValue

        result mustBe false
      }
    }

    ".getOssExclusion" - {

      "must return a Right(OssExcludedTrader) when OSS backend returns a successful valid payload" in {

        val ossExcludedTrader: OssExcludedTrader = arbOssExcludedTrader.copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = Some(currentDate.minusYears(2)),
          quarantined = Some(true)
        )

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(ossExcludedTrader).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.getOssExclusion(arbOssExcludedTrader.vrn.vrn).futureValue

        result mustBe ossExcludedTrader
      }

      "must throw an Exception when no registration details matching the vrn are retrieved" in {

        val message: String = "An error occurred whilst retrieving the OSS Excluded Trader with error: InternalServerError"

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.getOssExclusion(arbOssExcludedTrader.vrn.vrn)

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe message
        }
      }
    }

    ".isQuarantinedAndAfterTwoYears" - {

      "must throw an Illegal State Exception when no effective date is present" in {

        val updatedOssExcludedTrader = arbOssExcludedTrader.copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = None,
          quarantined = Some(true)
        )

        val message: String = "Expected effective date"

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val isQuarantinedAndAfterTwoYearsMethod = PrivateMethod[Boolean](Symbol("isQuarantinedAndAfterTwoYears"))

        intercept[IllegalStateException] {
          service invokePrivate isQuarantinedAndAfterTwoYearsMethod(currentDate, updatedOssExcludedTrader)
        }.getMessage mustBe message
      }

      "must return false when OSS Excluded Trader is not quarantined" in {

        val updatedOssExcludedTrader: OssExcludedTrader = arbOssExcludedTrader.copy(quarantined = Some(false))

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val isQuarantinedAndAfterTwoYearsMethod = PrivateMethod[Boolean](Symbol("isQuarantinedAndAfterTwoYears"))

        val result = service invokePrivate isQuarantinedAndAfterTwoYearsMethod(currentDate, updatedOssExcludedTrader)

        result mustBe false
      }

      "must return true when OSS Excluded Trader is quarantined and the effective date is after 2 years" in {

        val ossExcludedTrader = arbOssExcludedTrader.copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = Some(currentDate.minusYears(2).minusDays(1)),
          quarantined = Some(true)
        )

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val isQuarantinedAndAfterTwoYearsMethod = PrivateMethod[Boolean](Symbol("isQuarantinedAndAfterTwoYears"))

        val result = service invokePrivate isQuarantinedAndAfterTwoYearsMethod(currentDate, ossExcludedTrader)

        result mustBe true
      }

      "must return false when OSS Excluded Trader is quarantined and the effective date is before 2 years" in {

        val ossExcludedTrader = arbOssExcludedTrader.copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = Some(currentDate.minusYears(2)),
          quarantined = Some(true)
        )

        val service = OssExclusionsService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val isQuarantinedAndAfterTwoYearsMethod = PrivateMethod[Boolean](Symbol("isQuarantinedAndAfterTwoYears"))

        val result = service invokePrivate isQuarantinedAndAfterTwoYearsMethod(currentDate, ossExcludedTrader)

        result mustBe false
      }
    }
  }
}
