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

class EtmpCustomerIdentificationSpec extends SpecBase {

  private val etmpCustomerIdentification: EtmpCustomerIdentification = arbitraryEtmpCustomerIdentification.arbitrary.sample.value

  "EtmpCustomerIdentification" - {

    "must deserialise/serialise from and to EtmpCustomerIdentification" in {

      val json = Json.obj(
        "idType" -> etmpCustomerIdentification.idType,
        "idValue" -> etmpCustomerIdentification.idValue
      )

      val expectedResult: EtmpCustomerIdentification = EtmpCustomerIdentification(
        idType = etmpCustomerIdentification.idType,
        idValue = etmpCustomerIdentification.idValue
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[EtmpCustomerIdentification] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpCustomerIdentification] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "idValue" -> 123456
      )

      json.validate[EtmpCustomerIdentification] `mustBe` a[JsError]
    }
  }
}
