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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.iv.IdentityVerificationResult.Success
import models.iv.{IdentityVerificationUnexpectedResponse, NtcService, P60Service, PayslipService}
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

class IdentityVerificationConnectorSpec
  extends AnyFreeSpec
    with WireMockHelper
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val journeyId = "journeyId"

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.identity-verification.port" -> server.port)
      .build()


  ".getJourneyStatus" - {

    "when the response contains a state we recognise" - {

      "must return that response" in {

        val app = application

        val responseJson =
          """|{
             |  "progress": {
             |    "result": "Success"
             |  }
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(ok(responseJson))
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result.value mustBe Success
        }
      }
    }

    "when the response contains a state we don't recognise" - {

      "must return None" in {

        val app = application

        val responseJson =
          """|{
             |  "progress": {
             |    "result": "Unknown response"
             |  }
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(ok(responseJson))
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result mustBe empty
        }
      }
    }

    "when the call results in an error response code" - {

      "must return an Unexpected Response" in {

        val app = application

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(serverError())
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result.value mustBe IdentityVerificationUnexpectedResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }

  ".getDisabledEvidenceSource" - {

    "when the response contains only evidence sources we recognise" - {

      "must return a list of evidence sources" in {

        val app = application

        val validResponseJson =
          """|{
             |  "disabled-evidences": [
             |    "NtcService",
             |    "P60Service",
             |    "PayslipService"
             |  ]
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo("/identity-verification/disabled-evidences?origin=IOSS"))
              .willReturn(ok(validResponseJson))
          )

          val result = connector.getDisabledEvidenceSource().futureValue

          result must contain theSameElementsAs List(NtcService, P60Service, PayslipService)
        }
      }
    }

    "when the response contains some evidence sources we don't recognise" - {

      "must return a list of evidence sources we do recognise" in {

        val app = application

        val responseJsonWithUnexpectedSource =
          """|{
             |  "disabled-evidences": [
             |    "NtcService",
             |    "P60Service",
             |    "PayslipService",
             |    "OTHER"
             |  ]
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo("/identity-verification/disabled-evidences?origin=IOSS"))
              .willReturn(ok(responseJsonWithUnexpectedSource))
          )

          val result = connector.getDisabledEvidenceSource().futureValue

          result must contain theSameElementsAs List(NtcService, P60Service, PayslipService)
        }
      }
    }

    "when the call results in an error response code" - {

      "must return None" in {

        val app = application

        val errorResponseCode = Gen.oneOf(BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE).sample.value
        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo("/identity-verification/disabled-evidences?origin=IOSS"))
              .willReturn(status(errorResponseCode))
          )

          val result = connector.getDisabledEvidenceSource().futureValue

          result mustBe empty
        }
      }
    }
  }
}
