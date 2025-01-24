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


class EtmpAmendCustomerIdentificationSpec extends SpecBase {

  "EtmpAmendCustomerIdentification" - {

    "must serialize to JSON correctly" in {

      val etmpAmendCustomerIdentification = EtmpAmendCustomerIdentification(iossNumber)

      val expectedJson = Json.obj(
        "iossNumber" -> "IM9001234567"
      )

      Json.toJson(etmpAmendCustomerIdentification) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "iossNumber" -> "IM9001234567"
      )

      val etmpAmendCustomerIdentification = EtmpAmendCustomerIdentification(iossNumber)

      json.validate[EtmpAmendCustomerIdentification] mustBe JsSuccess(etmpAmendCustomerIdentification)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpAmendCustomerIdentification] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "iossNumber" -> 123456789
      )

      json.validate[EtmpAmendCustomerIdentification] mustBe a[JsError]
    }
  }

}

