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
import models.ossRegistration.OssRegistration
import models.responses.{InternalServerError, RegistrationNotFound}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class OssRegistrationServiceSpec extends SpecBase with PrivateMethodTester with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val arbOssRegistration: OssRegistration = arbitraryOssRegistration.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "OssRegistrationService" - {

    ".getLatestOssRegistration" - {

      "must return an OssRegistration when the connector returns a Right" in {

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(arbOssRegistration).toFuture

        val service = OssRegistrationService(mockRegistrationConnector)

        val result = service.getLatestOssRegistration(vrn).futureValue

        result mustBe Some(arbOssRegistration)
      }

      "must log an error and throw an exception when the connector returns a Left" in {

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Left(RegistrationNotFound).toFuture

        val exceptionMessage: String = s"An error occurred whilst retrieving the OSS Registration with error: $RegistrationNotFound"

        val service = OssRegistrationService(mockRegistrationConnector)

        val result = service.getLatestOssRegistration(vrn)

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe exceptionMessage
        }
      }
    }
  }
}
