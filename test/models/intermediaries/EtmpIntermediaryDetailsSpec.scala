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
import org.scalacheck.Gen
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpIntermediaryDetailsSpec extends SpecBase {

  private val etmpOtherIossIntermediaryRegistrationsList: Seq[EtmpOtherIossIntermediaryRegistrations] =
    Gen.listOfN(3, arbitraryOtherIossIntermediaryRegistrations.arbitrary).sample.value

  "EtmpIntermediaryDetails" - {

    "must deserialise/serialise from and to EtmpBankDetails" in {

      val json = Json.obj(
        "otherIossIntermediaryRegistrations" -> etmpOtherIossIntermediaryRegistrationsList
      )

      val expectedResult: EtmpIntermediaryDetails = EtmpIntermediaryDetails(
        otherIossIntermediaryRegistrations = etmpOtherIossIntermediaryRegistrationsList
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[EtmpIntermediaryDetails] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpIntermediaryDetails] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "otherIossIntermediaryRegistrations" -> 123456
      )

      json.validate[EtmpIntermediaryDetails] `mustBe` a[JsError]
    }
  }
}
