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

package models.audit

import models.UserAnswers
import models.requests.AuthenticatedDataRequest
import play.api.libs.json.{JsValue, Json}

case class RegistrationAuditModel(
                                   registrationAuditType: RegistrationAuditType,
                                   credId: String,
                                   userAgent: String,
                                   vrn: String,
                                   userAnswers: UserAnswers,
                                   submissionResult: SubmissionResult
                                 ) extends JsonAuditModel {

  override val auditType: String = registrationAuditType.auditType

  override val transactionName: String = registrationAuditType.transactionName

  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "userAnswersDetails" -> Json.toJson(userAnswers),
    "submissionResult" -> submissionResult
  )
}

object RegistrationAuditModel {

  def build(
             registrationAuditType: RegistrationAuditType,
             userAnswers: UserAnswers,
             submissionResult: SubmissionResult
           )(implicit request: AuthenticatedDataRequest[_]): RegistrationAuditModel =
    RegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = request.vrn.vrn,
      userAnswers = userAnswers,
      submissionResult = submissionResult
    )
}
