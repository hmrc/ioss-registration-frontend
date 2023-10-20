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
import models.etmp.TaxRefTraderID
import models.responses.etmp.EtmpEnrolmentResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDateTime

class RegistrationServiceSpec extends SpecBase with WireMockHelper with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  // TODO -> Populate a userAnswers model from arbitrary request to use here eqTo
  "must create a registration request from user answers provided and return a successful ETMP enrolment response" in {

    val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(
      processingDateTime = LocalDateTime.now(stubClockAtArbitraryDate),
      formBundleNumber = Some(arbitrary[String].sample.value),
      vrn = vrn.vrn,
      iossReference = arbitrary[TaxRefTraderID].sample.value.taxReferenceNumber,
      businessPartner = arbitrary[String].sample.value
    )

    when(mockRegistrationConnector.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

    val app = applicationBuilder(Some(completeUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
      .build()

    running(app) {

      registrationService.createRegistrationRequest(completeUserAnswersWithVatInfo, vrn).futureValue mustBe Right(etmpEnrolmentResponse)
      verify(mockRegistrationConnector, times(1)).createRegistration(any())(any())
    }
  }

  // TODO Left response test?
}