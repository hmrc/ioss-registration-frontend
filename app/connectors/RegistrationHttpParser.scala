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
import models.responses.{ConflictFound, ErrorResponse, UnexpectedResponseStatus}
import play.api.http.Status.{CONFLICT, CREATED, OK}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object RegistrationHttpParser extends Logging {

  type RegistrationResultResponse = Either[ErrorResponse, Unit]

  implicit object RegistrationResponseReads extends HttpReads[RegistrationResultResponse] {
    override def read(method: String, url: String, response: HttpResponse): RegistrationResultResponse =
      response.status match {
        case CREATED => Right(())
        case CONFLICT => Left(ConflictFound)
        case status => Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
  }

}

object AmendRegistrationHttpParser extends Logging {

  type AmendRegistrationResultResponse = Either[ErrorResponse, Any]

  implicit object AmendRegistrationResultResponseReads extends HttpReads[AmendRegistrationResultResponse] {
    override def read(method: String, url: String, response: HttpResponse): AmendRegistrationResultResponse =
      response.status match {
        case OK => Right(())
        case status => Left(UnexpectedResponseStatus(response.status, s"Unexpected amend response, status $status returned"))
      }
  }

}