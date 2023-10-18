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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsSuccess, Json}

class EtmpAdministrationSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpAdministration" - {

    "must serialise/deserialise to and from EtmpAdministration" in {

      val etmpAdministration = arbitrary[EtmpAdministration].sample.value

      val expectedJson = Json.obj(
        "messageType" -> s"${etmpAdministration.messageType}",
        "regimeId" -> s"${etmpAdministration.regimeId}"
      )

      Json.toJson(etmpAdministration) mustBe expectedJson
      expectedJson.validate[EtmpAdministration] mustBe JsSuccess(etmpAdministration)
    }
  }
}

