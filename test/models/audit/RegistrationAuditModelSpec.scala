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

import base.SpecBase
import models.requests.AuthenticatedDataRequest
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments

class RegistrationAuditModelSpec extends SpecBase {

  private val registrationAuditType: RegistrationAuditType = RegistrationAuditType.CreateRegistration
  private val submissionResult: SubmissionResult = SubmissionResult.values.head
  private val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

  "RegistrationAuditModelSpec" - {

    "must create correct json object" in {

      val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

      implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

      val registrationAuditModel = RegistrationAuditModel.build(
        registrationAuditType = registrationAuditType,
        userAnswers = emptyUserAnswers,
        etmpEnrolmentResponse = Some(etmpEnrolmentResponse),
        submissionResult = submissionResult
      )

      val expectedJson = Json.obj(
        "credId" -> request.credentials.providerId,
        "browserUserAgent" -> "",
        "requestersVrn" -> request.vrn.vrn,
        "userAnswersDetails" -> Json.toJson(emptyUserAnswers),
        "etmpEnrolmentResponse" -> Json.toJson(etmpEnrolmentResponse),
        "submissionResult" -> submissionResult
      )

      registrationAuditModel.detail mustBe expectedJson
    }
  }
}
