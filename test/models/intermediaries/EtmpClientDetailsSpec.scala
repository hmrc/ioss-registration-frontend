/*
 * Copyright 2026 HM Revenue & Customs
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

package models.intermediaries

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpClientDetailsSpec extends SpecBase {

  private val etmpClientDetails: EtmpClientDetails = arbitraryEtmpClientDetails.arbitrary.sample.value

  "EtmpClientDetails" - {

    "must deserialise/serialise from and to EtmpClientDetails" in {

      val json = Json.obj(
        "clientName" -> etmpClientDetails.clientName,
        "clientIossID" -> etmpClientDetails.clientIossID,
        "clientExcluded" -> etmpClientDetails.clientExcluded
      )

      val expectedResult: EtmpClientDetails = EtmpClientDetails(
        clientName = etmpClientDetails.clientName,
        clientIossID = etmpClientDetails.clientIossID,
        clientExcluded = etmpClientDetails.clientExcluded
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[EtmpClientDetails] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpClientDetails] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "clientExcluded" -> 123456
      )

      json.validate[EtmpClientDetails] `mustBe` a[JsError]
    }
  }
}
