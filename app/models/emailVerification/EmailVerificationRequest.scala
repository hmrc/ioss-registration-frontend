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

package models.emailVerification

import play.api.libs.json.{Json, OFormat}

case class EmailVerificationRequest(
                                     credId: String,
                                     continueUrl: String,
                                     origin: String,
                                     deskproServiceName: Option[String],
                                     accessibilityStatementUrl: String,
                                     pageTitle: Option[String],
                                     backUrl: Option[String],
                                     email: Option[VerifyEmail],
                                     lang: String = "en"
                                   )

object EmailVerificationRequest {

  implicit val format: OFormat[EmailVerificationRequest] = Json.format[EmailVerificationRequest]

}

case class VerifyEmail(
                        address: String,
                        enterUrl: String
                      )

object VerifyEmail {

  implicit val format: OFormat[VerifyEmail] = Json.format[VerifyEmail]

}
