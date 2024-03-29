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
import connectors.RegistrationHttpParser.{AmendRegistrationResultResponse, AmendRegistrationResultResponseReads, DisplayRegistrationResponse, DisplayRegistrationResponseReads, RegistrationResponseReads, RegistrationResultResponse}
import connectors.VatCustomerInfoHttpParser.{VatCustomerInfoResponse, VatCustomerInfoResponseReads}
import logging.Logging
import models.enrolments.EACDEnrolments
import models.etmp.EtmpRegistrationRequest
import models.etmp.amend.EtmpAmendRegistrationRequest
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class RegistrationConnector @Inject()(config: Configuration, httpClient: HttpClient)
                           (implicit executionContext: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-registration")

  def getVatCustomerInfo()(implicit hc: HeaderCarrier): Future[VatCustomerInfoResponse] = {
    httpClient.GET[VatCustomerInfoResponse](s"$baseUrl/vat-information")
  }

  def createRegistration(registrationRequest: EtmpRegistrationRequest)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    httpClient.POST[EtmpRegistrationRequest, RegistrationResultResponse](s"$baseUrl/create-registration", registrationRequest)
  }

  def amendRegistration(registrationRequest: EtmpAmendRegistrationRequest)(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    httpClient.POST[EtmpAmendRegistrationRequest, AmendRegistrationResultResponse](s"$baseUrl/amend", registrationRequest)
  }

  def getRegistration()(implicit hc: HeaderCarrier): Future[DisplayRegistrationResponse] =
    httpClient.GET[DisplayRegistrationResponse](s"$baseUrl/registration")

  def getRegistration(iossNumber: String)(implicit hc: HeaderCarrier): Future[DisplayRegistrationResponse] =
    httpClient.GET[DisplayRegistrationResponse](s"$baseUrl/registration/$iossNumber")

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] = {
    httpClient.GET[ExternalEntryUrlResponse](s"$baseUrl/external-entry")
  }

  def getAccounts()(implicit hc: HeaderCarrier): Future[EACDEnrolments] =
    httpClient.GET[EACDEnrolments](s"$baseUrl/accounts")
}
