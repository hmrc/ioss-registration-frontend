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

import models.domain.Registration
import models.requests.AuthenticatedDataRequest
import play.api.libs.json.{JsObject, JsValue, Json}

case class RegistrationAuditModel(
                                   registrationAuditType: RegistrationAuditType,
                                   credId: String,
                                   userAgent: String,
                                   registration: Registration,
                                   result: SubmissionResult
                                 ) extends JsonAuditModel {

  override val auditType: String = registrationAuditType.auditType
  override val transactionName: String = registrationAuditType.transactionName

  private val previousRegistrationDetail: JsObject =
    if (registration.previousRegistrations.nonEmpty) {
      Json.obj("previousRegistrations" -> Json.toJson(registration.previousRegistrations))
    } else {
      Json.obj()
    }

  private val websiteDetail: JsObject =
    if (registration.websites.nonEmpty) Json.obj("websites" -> registration.websites) else Json.obj()

  private val euRegistrationDetail: JsObject =
    if (registration.euRegistrations.nonEmpty) Json.obj("euRegistrations" -> Json.toJson(registration.euRegistrations)) else Json.obj()

  private val tradingNameDetail: JsObject =
    if (registration.tradingNames.nonEmpty) Json.obj("tradingNames" -> registration.tradingNames) else Json.obj()

  private val registrationDetail: JsValue = Json.obj(
    "vatRegistrationNumber" -> registration.vrn,
    "registeredCompanyName" -> registration.registeredCompanyName,
    "vatDetails" -> Json.toJson(registration.vatDetails),
    "contactDetails" -> Json.toJson(registration.contactDetails),
    "bankDetails" -> Json.toJson(registration.bankDetails)
  ) ++ tradingNameDetail ++
    euRegistrationDetail ++
    websiteDetail ++
    previousRegistrationDetail

  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "submissionResult" -> Json.toJson(result),
    "registrationDetails" -> registrationDetail
  )
}

object RegistrationAuditModel {

  def build(
             registrationAuditType: RegistrationAuditType,
             registration: Registration,
             result: SubmissionResult,
             request: AuthenticatedDataRequest[_]
           ): RegistrationAuditModel =
    RegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      registration = registration,
      result = result
    )
}
