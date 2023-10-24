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
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class EtmpMessageTypeSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpMessageType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpMessageType.values)

      forAll(gen) {
        etmpMessageType =>

          JsString(etmpMessageType.toString).validate[EtmpMessageType].asOpt.value mustBe etmpMessageType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpMessageType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpMessageType] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpMessageType.values)

      forAll(gen) {
        etmpMessageType =>

          Json.toJson(etmpMessageType) mustBe JsString(etmpMessageType.toString)
      }
    }
  }
}

