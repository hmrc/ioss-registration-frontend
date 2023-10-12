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
import connectors.EmailVerificationHttpParser.{ReturnEmailVerificationResponse, ReturnVerificationStatus, ReturnVerificationStatusReads, ReturnEmailVerificationReads}
import logging.Logging
import models.emailVerification.EmailVerificationRequest
import models.responses.UnexpectedResponseStatus
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpException}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationConnector @Inject()(
                                            config: Configuration,
                                            httpClient: HttpClient
                                          ) extends HttpErrorFunctions with Logging {

  private val baseUrl = config.get[Service]("microservice.services.email-verification")

  def getStatus(credId: String)
               (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnVerificationStatus] = {
    val url = s"$baseUrl/verification-status/$credId"
    httpClient.GET[ReturnVerificationStatus](url)
      .recover {
        case e: HttpException =>
          logger.error(s"VerificationStatus received an unexpected error with status: ${e.responseCode}")
          Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response, status ${e.responseCode} returned"))
      }
  }

  def verifyEmail(emailVerificationRequest: EmailVerificationRequest)
                 (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnEmailVerificationResponse] = {
    val url = s"${baseUrl}/verify-email"
    httpClient.POST[EmailVerificationRequest, ReturnEmailVerificationResponse](url, emailVerificationRequest)
      .recover {
        case e: HttpException =>
          logger.error(s"EmailVerificationResponse received an unexpected error with status: ${e.responseCode}")
          Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response, status ${e.responseCode} returned"))
      }
  }

}
