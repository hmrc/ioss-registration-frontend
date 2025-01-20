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
import models.emailVerification.PasscodeAttemptsStatus
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._


class PasscodeAttemptsStatusSpec extends AnyFreeSpec with Matchers with SpecBase with ScalaCheckPropertyChecks {

  "PasscodeAttemptsStatus" - {

    "must serialize and deserialize all valid values correctly" in {
      val validValues = PasscodeAttemptsStatus.values

      forAll(Gen.oneOf(validValues)) { status =>
        val json = JsString(status.toString)
        json.validate[PasscodeAttemptsStatus].asOpt.value mustEqual status
        Json.toJson(status) mustEqual json
      }
    }

    "must fail to deserialize invalid values" in {
      val invalidGen = Gen.alphaStr.suchThat(str => !PasscodeAttemptsStatus.values.map(_.toString).contains(str))

      forAll(invalidGen) { invalidValue =>
        JsString(invalidValue).validate[PasscodeAttemptsStatus] mustEqual JsError("error.invalid")
      }
    }

    "must return the correct set of values" in {
      PasscodeAttemptsStatus.values must contain allOf(
        PasscodeAttemptsStatus.LockedPasscodeForSingleEmail,
        PasscodeAttemptsStatus.LockedTooManyLockedEmails,
        PasscodeAttemptsStatus.Verified,
        PasscodeAttemptsStatus.NotVerified
      )
    }

    "must support Enumerable instance" in {
      val gen = Gen.oneOf(PasscodeAttemptsStatus.values)

      forAll(gen) { status =>
        PasscodeAttemptsStatus.enumerable.withName(status.toString).value mustEqual status
      }

      PasscodeAttemptsStatus.enumerable.withName("nonexistent") mustBe empty
    }
  }
}
