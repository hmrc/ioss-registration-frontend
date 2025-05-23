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

import config.Service
import connectors.ExternalEntryUrlHttpParser.{ExternalEntryUrlResponse, ExternalEntryUrlResponseReads}
import connectors.RegistrationHttpParser._
import connectors.VatCustomerInfoHttpParser.{VatCustomerInfoResponse, VatCustomerInfoResponseReads}
import logging.Logging
import models.enrolments.EACDEnrolments
import models.etmp.EtmpRegistrationRequest
import models.etmp.amend.EtmpAmendRegistrationRequest
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}
import  uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class RegistrationConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                     (implicit executionContext: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-registration")

  def getVatCustomerInfo()(implicit hc: HeaderCarrier): Future[VatCustomerInfoResponse] =
    httpClientV2.get(url"$baseUrl/vat-information").execute[VatCustomerInfoResponse]

  def createRegistration(registrationRequest: EtmpRegistrationRequest)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] =
    httpClientV2.post(url"$baseUrl/create-registration").withBody(Json.toJson(registrationRequest)).execute[RegistrationResultResponse]

  def amendRegistration(registrationRequest: EtmpAmendRegistrationRequest)(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] =
    httpClientV2.post(url"$baseUrl/amend").withBody(Json.toJson(registrationRequest)).execute[AmendRegistrationResultResponse]

  def getRegistration()(implicit hc: HeaderCarrier): Future[DisplayRegistrationResponse] =
    httpClientV2.get(url"$baseUrl/registration").execute[DisplayRegistrationResponse]

  def getRegistration(iossNumber: String)(implicit hc: HeaderCarrier): Future[DisplayRegistrationResponse] =
    httpClientV2.get(url"$baseUrl/registration/$iossNumber").execute[DisplayRegistrationResponse]

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] =
    httpClientV2.get(url"$baseUrl/external-entry").execute[ExternalEntryUrlResponse]

  def getAccounts()(implicit hc: HeaderCarrier): Future[EACDEnrolments] =
    httpClientV2.get(url"$baseUrl/accounts").execute[EACDEnrolments]

  def getOssRegistrationExclusion(vrn: Vrn)(implicit hc: HeaderCarrier): Future[OssDisplayRegistrationResponse] = {
    val baseUrl: Service = config.get[Service]("microservice.services.one-stop-shop-registration")

    httpClientV2.get(url"$baseUrl/registration/$vrn").execute[OssDisplayRegistrationResponse]
  }

  def getOssRegistration(vrn: Vrn)(implicit hc: HeaderCarrier): Future[OssRegistrationResponse] = {
    val baseUrl: Service = config.get[Service]("microservice.services.one-stop-shop-registration")

    httpClientV2.get(url"$baseUrl/registration/$vrn").execute[OssRegistrationResponse]
  }
}
