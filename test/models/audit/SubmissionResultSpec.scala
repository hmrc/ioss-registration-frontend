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

package models.audit

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class SubmissionResultSpec extends SpecBase with ScalaCheckPropertyChecks {

  "SubmissionResultS" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SubmissionResult.values)

      forAll(gen) {
        submissionResult =>

          JsString(submissionResult.toString).validate[SubmissionResult].asOpt.value mustBe submissionResult
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!SubmissionResult.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[SubmissionResult] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SubmissionResult.values)

      forAll(gen) {
        submissionResult =>

          Json.toJson(submissionResult) mustBe JsString(submissionResult.toString)
      }
    }
  }
}
