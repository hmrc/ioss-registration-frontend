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

package models.emailVerfication

import base.SpecBase
import models.emailVerification.EmailVerificationResponse
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}


class EmailVerificationResponseSpec extends AnyFreeSpec with Matchers with SpecBase {

  "EmailVerificationResponse" - {

    "must serialise and deserialise to and from a EmailVerificationResponse" - {

      "with all optional fields present" in {

        val emailVerificationResponse: EmailVerificationResponse =
          EmailVerificationResponse(
            "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-details"
          )

        val expectedJson = Json.obj(
          "redirectUri" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-details"
        )

        Json.toJson(emailVerificationResponse) mustEqual expectedJson
        expectedJson.validate[EmailVerificationResponse] mustEqual JsSuccess(emailVerificationResponse)
      }

    }
  }

}
