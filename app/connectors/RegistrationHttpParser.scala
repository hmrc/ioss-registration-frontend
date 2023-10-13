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
import models.responses.etmp.EtmpEnrolmentResponse
import models.responses._
import play.api.http.Status.{CONFLICT, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object RegistrationHttpParser extends Logging {

  type RegistrationResultResponse = Either[ErrorResponse, EtmpEnrolmentResponse]

  implicit object RegistrationResponseReads extends HttpReads[RegistrationResultResponse] {
    override def read(method: String, url: String, response: HttpResponse): RegistrationResultResponse =
      response.status match {
        case CREATED => response.json.validate[EtmpEnrolmentResponse] match {
          case JsSuccess(enrolmentResponse, _) => Right(enrolmentResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse JSON, but was successfully created ${response.body} ${errors}", errors)
            Left(InvalidJson)
        }
        case CONFLICT =>
          logger.error(s"Received ConflictFound when trying to submit registration")
          Left(ConflictFound)
        case INTERNAL_SERVER_ERROR =>
          logger.error(s"Received InternalServerError when trying to submit registration with message: ${InternalServerError.body}")
          Left(InternalServerError)
        case status =>
          logger.error(s"Received unexpected error when trying to submit registration with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
  }
}
