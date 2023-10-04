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

import logging.Logging
import play.api.http.Status.OK
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

case class EmailPasscodeEntry(email: String, passcode: String)

object EmailPasscodeEntry {
  implicit val format: Format[EmailPasscodeEntry] = Json.format[EmailPasscodeEntry]
}

case class EmailPasscodes(passcodes: Seq[EmailPasscodeEntry])

object EmailPasscodes {
  implicit val format: Format[EmailPasscodes] = Json.format[EmailPasscodes]
}

object TestOnlyEmailPasscodeHttpParser extends Logging {

  type TestOnlyEmailPasscodeResponse = Either[ServiceError, EmailPasscodes]

  implicit object TestOnlyEmailPasscodeReads extends HttpReads[TestOnlyEmailPasscodeResponse] {
    override def read(method: String, url: String, response: HttpResponse): TestOnlyEmailPasscodeResponse = {
      response.status match {
        case OK =>
          response.json.validate[EmailPasscodes] match {
            case JsSuccess(value, _) => Right(value)
            case JsError(errors) => {
              val errorMessage = s"Unable to read json got errors $errors"
              Left(DownstreamServiceError(errorMessage, FailedToFetchTestOnlyPasscode(errorMessage)))
            }
          }
        case _ =>
          logger.error(s"Unable to find test only passcodes with status ${response.status} with body ${response.body}")
          Left(DownstreamServiceError(s"Received unexpected response code ${response.status}",
            FailedToFetchTestOnlyPasscode("Failed to get test only passcodes")))
      }
    }
  }

}

case class FailedToFetchTestOnlyPasscode(message: String) extends Exception
