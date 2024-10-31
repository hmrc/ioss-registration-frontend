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

package controllers.ossExclusions

import base.SpecBase
import controllers.ossExclusions.{routes => ossExcludedRoutes}
import formats.Format.quarantinedOSSRegistrationFormatter
import models.ossExclusions.{ExclusionReason, OssExcludedTrader}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.oss.OssExclusionsService
import utils.FutureSyntax.FutureOps
import views.html.ossExclusions.CannotRegisterQuarantinedTraderView

import java.time.LocalDate

class CannotRegisterQuarantinedTraderControllerSpec extends SpecBase {

  private val mockOssExclusionsService: OssExclusionsService = mock[OssExclusionsService]

  private val arbOssExcludedTrader: OssExcludedTrader =
    OssExcludedTrader(
      vrn = vrn,
      exclusionReason = Some(ExclusionReason.FailsToComply),
      effectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate)),
      quarantined = Some(true)
    )

  "CannotRegisterQuarantinedTrader Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockOssExclusionsService.getOssExclusion(any())(any())) thenReturn arbOssExcludedTrader.toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, ossExcludedRoutes.CannotRegisterQuarantinedTraderController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRegisterQuarantinedTraderView]

        val formattedExclusionEndDate: String = arbOssExcludedTrader
          .effectiveDate.map(_.plusYears(2).plusDays(1).format(quarantinedOSSRegistrationFormatter)).get

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(formattedExclusionEndDate)(request, messages(application)).toString
      }
    }

    "must throw an IllegalStateException when Oss Excluded Trader effective date is missing" in {

      val exceptionMessage: String = "Expected effective date"

      val arbOssExcludedTraderWithoutOptionalDate: OssExcludedTrader = arbOssExcludedTrader.copy(
        effectiveDate = None
      )

      when(mockOssExclusionsService.getOssExclusion(any())(any())) thenReturn arbOssExcludedTraderWithoutOptionalDate.toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, ossExcludedRoutes.CannotRegisterQuarantinedTraderController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[IllegalStateException]
          exp.getMessage mustBe exceptionMessage
        }
      }
    }
  }
}
