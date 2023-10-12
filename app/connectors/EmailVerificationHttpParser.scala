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

import logging.Logging
import models.emailVerification.{EmailVerificationResponse, VerificationStatus}
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EmailVerificationHttpParser extends Logging {

  type ReturnEmailVerificationResponse = Either[ErrorResponse, EmailVerificationResponse]
  type ReturnVerificationStatus = Either[ErrorResponse, Option[VerificationStatus]]

  implicit object ReturnEmailVerificationReads extends HttpReads[ReturnEmailVerificationResponse] {

    override def read(method: String, url: String, response: HttpResponse): ReturnEmailVerificationResponse = {
      response.status match {
        case CREATED =>
          response.json.validate[EmailVerificationResponse] match {
            case JsSuccess(verifyEmail, _) => Right(verifyEmail)
            case JsError(errors) =>
              logger.error(s"EmailVerificationResponse: ${response.json}, failed to parse with errors: $errors.")
              Left(InvalidJson)
          }
        case status =>
          logger.error(s"EmailVerificationResponse received an unexpected error with status: ${response.status}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  implicit object ReturnVerificationStatusReads extends HttpReads[ReturnVerificationStatus] {

    override def read(method: String, url: String, response: HttpResponse): ReturnVerificationStatus = {
      response.status match {
        case OK =>
          response.json.validate[VerificationStatus] match {
            case JsSuccess(verificationStatus, _) => Right(Some(verificationStatus.toVerificationStatus))
            case JsError(errors) =>
              logger.error(s"VerificationStatus: ${response.json}, failed to parse with errors: $errors")
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn(s"VerificationStatus received NOT_FOUND with status: ${response.status}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status ${response.status} returned"))

        case status =>
          logger.error(s"VerificationStatus received an unexpected error with status: ${response.status}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

}

