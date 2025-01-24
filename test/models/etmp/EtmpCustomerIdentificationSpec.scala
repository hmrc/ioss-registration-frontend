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

package models.etmp

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpCustomerIdentificationSpec extends SpecBase {

  "EtmpCustomerIdentification" - {

    "must serialise/deserialise to and from EtmpCustomerIdentification" in {

      val etmpCustomerIdentification = arbitrary[EtmpCustomerIdentification].sample.value

      val expectedJson = Json.obj(
        "vrn" -> s"${etmpCustomerIdentification.vrn.vrn}"
      )

      Json.toJson(etmpCustomerIdentification) mustBe expectedJson
      expectedJson.validate[EtmpCustomerIdentification] mustBe JsSuccess(etmpCustomerIdentification)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[EtmpCustomerIdentification] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "vrn" -> 123456789
      )

      expectedJson.validate[EtmpCustomerIdentification] mustBe a[JsError]
    }
  }
}

