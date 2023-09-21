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

package forms.mappings

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.validation.{Invalid, Valid}

class IossRegistrationNumberConstraintsSpec extends AnyFreeSpec with Matchers with IossRegistrationNumberConstraints {

  "validateIossRegistrationNumber" - {

    "must return Valid when correct ioss registration number is passed" in {
      val result = validateIossRegistrationNumber("AT", "previousIossNumber.error.invalid")("IM0401234567")
      result mustEqual Valid
    }

    "must return Invalid when incorrect ioss registration number is passed" in {
      val result = validateIossRegistrationNumber("AT", "previousIossNumber.error.invalid")("IK0401234567")
      result mustEqual Invalid("previousIossNumber.error.invalid")
    }
  }

}
