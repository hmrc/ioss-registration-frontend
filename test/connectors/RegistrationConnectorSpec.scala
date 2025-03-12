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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.amend.RegistrationWrapper
import models.domain.VatCustomerInfo
import models.enrolments.EACDEnrolments
import models.external.ExternalEntryUrl
import models.ossExclusions.{ExclusionReason, OssExcludedTrader}
import models.ossRegistration.OssRegistration
import models.responses.*
import models.responses.etmp.EtmpEnrolmentResponse
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.running
import testutils.RegistrationData.{amendRegistrationResponse, etmpAmendRegistrationRequest, etmpDisplayRegistration, etmpRegistrationRequest}
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate


class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    applicationBuilder()
      .configure(
        "microservice.services.ioss-registration.port" -> server.port,
        "microservice.services.one-stop-shop-registration.port" -> server.port,
      ).build()

  ".getCustomerVatInfo" - {

    val url: String = "/ioss-registration/vat-information"

    "must return vat information when the backend returns some" in {

      running(application) {
        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val vatInfo: VatCustomerInfo = vatCustomerInfo

        val responseBody = Json.toJson(vatInfo).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getVatCustomerInfo().futureValue

        result mustBe Right(vatInfo)
      }
    }

    "must return invalid json when the backend returns some" in {

      running(application) {
        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseBody = Json.obj("test" -> "test").toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getVatCustomerInfo().futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Left(NotFound) when the backend returns NOT_FOUND" in {

      running(application) {
        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

        val result = connector.getVatCustomerInfo().futureValue

        result mustBe Left(NotFound)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the backend returns another error code" in {

      val status = Gen.oneOf(BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE).sample.value

      running(application) {
        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

        val result = connector.getVatCustomerInfo().futureValue

        result mustBe Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
      }
    }
  }

  "getSavedExternalEntry" - {

    val url = "/ioss-registration/external-entry"

    "must return information when the backend returns some" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        val externalEntryResponse = ExternalEntryUrl(Some("/url"))

        val responseBody = Json.toJson(externalEntryResponse).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Right(externalEntryResponse)
      }
    }

    "must return invalid json when the backend returns some" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        val responseBody = Json.obj("test" -> "test").toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Right(ExternalEntryUrl(None))
      }
    }

    "must return Left(NotFound) when the backend returns NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Left(NotFound)
      }
    }

    "must return Left(UnexpectedStatus) when the backend returns another error code" in {

      val status = Gen.oneOf(400, 500, 501, 502, 503).sample.value

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status with body "))
      }
    }
  }

  ".createRegistration" - {

    val url: String = "/ioss-registration/create-registration"

    "must return CREATED with a valid ETMP Enrolment Response when backend returns CREATED with a valid response body" in {

      running(application) {

        val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

        val responseBody = Json.toJson(etmpEnrolmentResponse).toString()

        server.stubFor(post(urlEqualTo(url))
          .willReturn(aResponse.withStatus(CREATED)
            .withBody(responseBody)))

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Right(etmpEnrolmentResponse)
      }
    }

    "must return Left(InvalidJson) when the response body is not a valid ETMP Enrolment Response Json" in {

      running(application) {

        server.stubFor(post(urlEqualTo(url))
          .willReturn(aResponse.withStatus(CREATED)
            .withBody(Json.toJson("test").toString())))

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Conflict when backend responds with Conflict" in {

      running(application) {

        server.stubFor(post(urlEqualTo(url))
          .willReturn(aResponse.withStatus(CONFLICT)))

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Left(ConflictFound)
      }
    }

    "must return Internal Server Error when backend responds with Internal Server Error" in {

      running(application) {

        server.stubFor(post(urlEqualTo(url))
          .willReturn(aResponse.withStatus(INTERNAL_SERVER_ERROR)))

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Left(InternalServerError)
      }
    }

    "must return UnexpectedResponseStatus when any other error is returned" in {

      val status = 123

      running(application) {

        server.stubFor(post(urlEqualTo(url))
          .willReturn(aResponse.withStatus(status)))

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Left(UnexpectedResponseStatus(status, s"Unexpected response, status $status returned"))
      }
    }
  }

  ".getRegistration" - {

    val url = "/ioss-registration/registration"

    "must return a registration wrapper when the backend successfully returns one" in {

      val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseBody = Json.toJson(registrationWrapper).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getRegistration().futureValue

        result mustBe Right(registrationWrapper)
      }
    }

    "must return an Internal Server Error when the backend responds with Internal Server Error" in {

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url))
          .willReturn(aResponse.withStatus(INTERNAL_SERVER_ERROR)))

        val result = connector.getRegistration().futureValue

        result mustBe Left(InternalServerError)
      }
    }
  }

  "amendRegistration" - {
    val url = s"/ioss-registration/amend"

    "must return Right when a new registration is created on the backend" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        val response = Json.toJson(amendRegistrationResponse).toString()
        server.stubFor(post(urlEqualTo(url)).willReturn(ok(response)))

        val result = connector.amendRegistration(etmpAmendRegistrationRequest).futureValue

        result mustBe Right(amendRegistrationResponse)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the backend returns UnexpectedResponseStatus" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(123)))

        val result = connector.amendRegistration(etmpAmendRegistrationRequest).futureValue

        result mustBe Left(UnexpectedResponseStatus(123, "Unexpected amend response, status 123 returned"))
      }
    }
  }

  ".getAccounts" - {
    val url = s"/ioss-registration/accounts"

    "must return a registration when the server provides one" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val eACDEnrolments = arbitrary[EACDEnrolments].sample.value

        val responseBody = Json.toJson(eACDEnrolments).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getAccounts().futureValue

        result mustEqual eACDEnrolments
      }
    }

  }

  ".getOssRegistrationExclusion" - {

    val ossUrl = s"/one-stop-shop-registration/registration/$vrn"

    "must return OK with a valid OssExcludedTrader when OSS backend returns OK with a valid response body" in {

      val exclusionEffectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

      val ossExcludedTrader: OssExcludedTrader = arbitraryOssExcludedTrader.arbitrary.sample.value
        .copy(
          exclusionReason = Some(ExclusionReason.FailsToComply),
          effectiveDate = Some(exclusionEffectiveDate),
          quarantined = Some(true)
        )

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseJson =
          s"""{
             | "vrn" : ${Json.toJson(ossExcludedTrader.vrn)},
             | "excludedTrader": {
             |    "exclusionReason" :${Json.toJson(ossExcludedTrader.exclusionReason)},
             |    "effectiveDate" : ${Json.toJson(ossExcludedTrader.effectiveDate)},
             |    "quarantined" : ${Json.toJson(ossExcludedTrader.quarantined)}
             |    }
             |}""".stripMargin

        server.stubFor(get(urlEqualTo(ossUrl))
          .willReturn(ok().withBody(responseJson))
        )

        val result = connector.getOssRegistrationExclusion(vrn).futureValue

        result mustBe Right(ossExcludedTrader)
      }
    }

    "must return InvalidJson when OSS backend returns invalid JSON" in {

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val responseJson = Json.obj("test" -> "test").toString()

        server.stubFor(get(urlEqualTo(ossUrl))
          .willReturn(ok().withBody(responseJson))
        )

        val result = connector.getOssRegistrationExclusion(vrn).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return an Internal Server Error when OSS backend responds with Internal Server Error" in {

      running(application) {

        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(ossUrl))
          .willReturn(aResponse.withStatus(INTERNAL_SERVER_ERROR))
        )

        val result = connector.getOssRegistrationExclusion(vrn).futureValue

        result mustBe Left(InternalServerError)
      }
    }
  }

  ".getOssRegistration" - {

    val ossUrl = s"/one-stop-shop-registration/registration/$vrn"

    "must return a oss registration when the server provides one" in {

      running(application) {
        val connector = application.injector.instanceOf[RegistrationConnector]
        val ossRegistration = arbitrary[OssRegistration].sample.value

        val responseBody = Json.toJson(ossRegistration).toString

        server.stubFor(get(urlEqualTo(ossUrl)).willReturn(ok().withBody(responseBody)))

        val result = connector.getOssRegistration(vrn).futureValue

        result mustBe Right(ossRegistration)
      }
    }
  }
}
