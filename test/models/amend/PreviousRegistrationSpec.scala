/*
 * Copyright 2024 HM Revenue & Customs
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

package models.amend

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, JsSuccess, Json}

// TODO
class PreviousRegistrationSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val previousRegistration: PreviousRegistration = arbitraryPreviousRegistration.arbitrary.sample.value

  "PreviousRegistration" - {

    "must serialise/deserialise to and from PreviousRegistration" in {

      val expectedJson = Json.obj(
        "iossNumber" -> previousRegistration.iossNumber,
        "startPeriod" -> previousRegistration.startPeriod,
        "endPeriod" -> previousRegistration.endPeriod
      )

      Json.toJson(previousRegistration) mustBe expectedJson
      expectedJson.validate[PreviousRegistration] mustBe JsSuccess(previousRegistration)
    }

    "options????" in {
      // TODO -> Test RadioItems???
    }
  }
}
