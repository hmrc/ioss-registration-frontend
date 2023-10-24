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
import connectors.RegistrationConnector
import formats.Format.dateFormatter
import models.responses.etmp.EtmpEnrolmentResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RegistrationService
import testutils.RegistrationData.etmpRegistrationRequest
import utils.FutureSyntax.FutureOps
import views.html.ApplicationCompleteView

import java.time.LocalDateTime

class ApplicationCompleteControllerSpec extends SpecBase {

//  private val iossReference: String = arbitrary[String].sample.value
//  private val organisationName: String = vatCustomerInfo.organisationName.get
  private val date: String = arbitraryDate.format(dateFormatter)

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  private val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(
    processingDateTime = LocalDateTime.now(stubClockAtArbitraryDate),
    formBundleNumber = None,
    vrn = vrn.vrn,
    iossReference = "123456789",
    businessPartner = "businessPartner"
  )

  // TODO
  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationConnector.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture
      when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          etmpEnrolmentResponse.iossReference,
          vatCustomerInfo.organisationName.get,
          date,
          date,
          date,
          config.feedbackUrl(request)
        )(request, messages(application)).toString
      }
    }
  }
}
