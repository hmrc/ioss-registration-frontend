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

import models.core.{CoreRegistrationRequest, CoreRegistrationValidationResult}
import models.requests.AuthenticatedDataRequest
import play.api.libs.json.{Json, JsValue}

case class CoreRegistrationAuditModel(
                                       credId: String,
                                       userAgent: String,
                                       vrn: String,
                                       coreRegistrationRequest: CoreRegistrationRequest,
                                       coreRegistrationValidationResult: CoreRegistrationValidationResult
                                     ) extends JsonAuditModel {

  override val auditType: String = "CoreRegistrationValidation"

  override val transactionName: String = "core-registration-validation"


  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "coreRegistrationRequest" -> Json.toJson(coreRegistrationRequest),
    "coreRegistrationValidationResponse" -> Json.toJson(coreRegistrationValidationResult)
  )
}

object CoreRegistrationAuditModel {

  def build(
             coreRegistrationRequest: CoreRegistrationRequest,
             coreRegistrationValidationResult: CoreRegistrationValidationResult
           )(implicit request: AuthenticatedDataRequest[_]): CoreRegistrationAuditModel =
    CoreRegistrationAuditModel(
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      request.vrn.vrn,
      coreRegistrationRequest: CoreRegistrationRequest,
      coreRegistrationValidationResult: CoreRegistrationValidationResult
    )
}
