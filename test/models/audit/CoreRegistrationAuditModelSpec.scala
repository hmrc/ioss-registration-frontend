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
import models.core._
import models.requests.AuthenticatedDataRequest
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

import java.time.LocalDate

class CoreRegistrationAuditModelSpec extends SpecBase with Matchers {

  "CoreRegistrationAuditModel" - {

    "must create correct json object" in {

      val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers)

      implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, None, emptyUserAnswers)

      val coreRegistrationRequest = CoreRegistrationRequest(SourceType.VATNumber.toString, None, vrn.vrn, None, "GB")

      val coreRegistrationValidationResult: CoreRegistrationValidationResult =
        CoreRegistrationValidationResult(
          "IM2344433220",
          Some("IN4747493822"),
          "FR",
          traderFound = true,
          Seq(Match(
            MatchType.FixedEstablishmentQuarantinedNETP,
            "IM0987654321",
            Some("444444444"),
            "DE",
            Some(3),
            Some(LocalDate.now()),
            Some(LocalDate.now()),
            Some(1),
            Some(2)
          ))
        )

      val coreRegistrationAuditModel = CoreRegistrationAuditModel.build(coreRegistrationRequest, coreRegistrationValidationResult)

      val expectedJson = Json.obj(
        "credId" -> request.credentials.providerId,
        "browserUserAgent" -> "",
        "requestersVrn" -> request.vrn.vrn,
        "coreRegistrationRequest" -> coreRegistrationRequest,
        "coreRegistrationValidationResponse" -> coreRegistrationValidationResult
      )
      coreRegistrationAuditModel.detail mustEqual expectedJson
    }
  }

}
