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

package connectors.test

import config.Service
import connectors.test.TestOnlyEmailPasscodeHttpParser.{TestOnlyEmailPasscodeReads, TestOnlyEmailPasscodeResponse}
import logging.Logging
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyEmailPasscodeConnector @Inject()(
                                                httpClient: HttpClient,
                                                config: Configuration
                                              )(implicit ec: ExecutionContext) extends Logging {

  private val service = config.get[Service]("microservice.services.email-verification")

  private val getTestOnlyPasscodeUrl: String =
    s"${service.protocol}://${service.host}:${service.port}/test-only/passcodes"

  def getTestOnlyPasscode()(implicit hc: HeaderCarrier): Future[TestOnlyEmailPasscodeResponse] = {
    httpClient.GET[TestOnlyEmailPasscodeResponse](getTestOnlyPasscodeUrl)
  }
}
