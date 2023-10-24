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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import formats.Format.{dateFormatter, dateMonthYearFormatter}
import models.UserAnswers
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.etmp.EtmpEnrolmentResponseQuery
import views.html.ApplicationCompleteView

import java.time.{LocalDate, LocalDateTime}

class ApplicationCompleteControllerSpec extends SpecBase {

  private val commencementDate = LocalDate.now(stubClockAtArbitraryDate)
  private val returnStartDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
  private val includedSalesDate = commencementDate.withDayOfMonth(1)

  private val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(
    processingDateTime = LocalDateTime.now(stubClockAtArbitraryDate),
    formBundleNumber = None,
    vrn = vrn.vrn,
    iossReference = "123456789",
    businessPartner = "businessPartner"
  )

  private val userAnswers: UserAnswers = completeUserAnswersWithVatInfo
    .set(EtmpEnrolmentResponseQuery, etmpEnrolmentResponse).success.value

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          etmpEnrolmentResponse.iossReference,
          vatCustomerInfo.organisationName.get,
          includedSalesDate.format(dateMonthYearFormatter),
          returnStartDate.format(dateFormatter),
          includedSalesDate.format(dateFormatter),
          config.feedbackUrl(request)
        )(request, messages(application)).toString
      }
    }
  }
}
