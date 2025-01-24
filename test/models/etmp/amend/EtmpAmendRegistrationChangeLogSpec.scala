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

package models.etmp.amend

import base.SpecBase
import play.api.libs.json.*


class EtmpAmendRegistrationChangeLogSpec extends SpecBase {

  "AmendRegistrationResponse" - {

    "must serialize to JSON correctly" in {

      val changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = true,
        fixedEstablishments = true,
        contactDetails = true,
        bankDetails = true,
        reRegistration = false
      )

      val expectedJson = Json.obj(
        "reRegistration" -> false,
        "tradingNames" -> true,
        "contactDetails" -> true,
        "fixedEstablishments" -> true,
        "bankDetails" -> true
      )

      Json.toJson(changeLog) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "reRegistration" -> false,
        "tradingNames" -> true,
        "contactDetails" -> true,
        "fixedEstablishments" -> true,
        "bankDetails" -> true
      )

      val changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = true,
        fixedEstablishments = true,
        contactDetails = true,
        bankDetails = true,
        reRegistration = false
      )

      json.validate[EtmpAmendRegistrationChangeLog] mustBe JsSuccess(changeLog)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpAmendRegistrationChangeLog] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "reRegistration" -> 12345,
        "tradingNames" -> true,
        "contactDetails" -> true,
        "fixedEstablishments" -> true,
        "bankDetails" -> true
      )

      json.validate[EtmpAmendRegistrationChangeLog] mustBe a[JsError]
    }
  }

}

