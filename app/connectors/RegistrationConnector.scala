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
import connectors.RegistrationHttpParser.{RegistrationResponseReads, RegistrationResultResponse}
import connectors.VatCustomerInfoHttpParser.{VatCustomerInfoResponse, VatCustomerInfoResponseReads}
import play.api.Configuration
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class RegistrationConnector @Inject()(config: Configuration, httpClient: HttpClient)
                           (implicit executionContext: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-registration")
  def getVatCustomerInfo()(implicit hc: HeaderCarrier): Future[VatCustomerInfoResponse] = {
    httpClient.GET[VatCustomerInfoResponse](s"$baseUrl/vat-information")
  }

  // TODO -> send etmpRegRequest
  def createRegistration(string: String)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    httpClient.POST[String, RegistrationResultResponse](s"$baseUrl/create", string)
  }.map {
    result => result
  }

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] = {
    httpClient.GET[ExternalEntryUrlResponse](s"$baseUrl/external-entry")
  }
}
