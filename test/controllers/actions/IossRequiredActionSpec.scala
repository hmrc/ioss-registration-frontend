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

package controllers.actions

import base.SpecBase
import connectors.RegistrationConnector
import models.amend.RegistrationWrapper
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testutils.RegistrationData.etmpDisplayRegistration
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IossRequiredActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(connector: RegistrationConnector) extends IossRequiredActionImpl(connector) {

    def callRefine[A](request: AuthenticatedDataRequest[A]):
    Future[Either[Result, AuthenticatedMandatoryIossRequest[A]]] = refine(request)
  }

  private val mockRegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "Ioss Required Action" - {

    "when the user has logged in as an Organisation Admin with strong credentials but ioss enrolment" - {

      "must return Unauthorized" in {

        val action = new Harness(mockRegistrationConnector)
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthenticatedDataRequest(
          request,
          testCredentials,
          vrn,
          None,
          emptyUserAnswersWithVatInfo,
          None
        )).futureValue

        result mustBe Left(Unauthorized)
        verifyZeroInteractions(mockRegistrationConnector)
      }

      "must return InternalServerError" in {
        when(mockRegistrationConnector.getRegistration()(any())) thenReturn
          Left(models.responses.InternalServerError).toFuture

        val action = new Harness(mockRegistrationConnector)
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthenticatedDataRequest(
          request,
          testCredentials,
          vrn,
          Some(iossNumber),
          emptyUserAnswersWithVatInfo,
          None
        )).futureValue

        result mustBe Left(InternalServerError)
      }

      "must return Right" in {

        val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

        val action = new Harness(mockRegistrationConnector)
        val request = AuthenticatedDataRequest(
          FakeRequest(GET, "/test/url?k=session-id"),
          testCredentials,
          vrn,
          Some(iossNumber),
          emptyUserAnswersWithVatInfo,
          None
        )

        val result = action.callRefine(request).futureValue

        val expectResult = AuthenticatedMandatoryIossRequest(
          request,
          testCredentials,
          vrn,
          iossNumber,
          registrationWrapper,
          emptyUserAnswersWithVatInfo
        )

        result mustBe Right(expectResult)
      }
    }
  }
}
