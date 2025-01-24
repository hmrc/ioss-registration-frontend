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

package models

import base.SpecBase
import config.Constants.correctionsPeriodsLimit
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.time.LocalDate

class ReturnSpec extends SpecBase with ScalaFutures {

  "Return" - {

    "must serialize to JSON correctly" in {
      val dueReturn = Return(
        firstDay = LocalDate.now(),
        lastDay = LocalDate.now(),
        dueDate = LocalDate.now().minusYears(correctionsPeriodsLimit - 1),
        submissionStatus = SubmissionStatus.Due,
        inProgress = true,
        isOldest = true
      )

      val json = Json.obj(
        "lastDay" -> s"${LocalDate.now()}",
        "isOldest" -> true,
        "firstDay" -> s"${LocalDate.now()}",
        "dueDate" -> s"${LocalDate.now().minusYears(correctionsPeriodsLimit - 1)}",
        "submissionStatus" -> "DUE",
        "inProgress" -> true
      )

      Json.toJson(dueReturn) mustBe json
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "lastDay" -> s"${LocalDate.now()}",
        "isOldest" -> true,
        "firstDay" -> s"${LocalDate.now()}",
        "dueDate" -> s"${LocalDate.now().minusYears(correctionsPeriodsLimit - 1)}",
        "submissionStatus" -> "DUE",
        "inProgress" -> true
      )

      val dueReturn = Return(
        firstDay = LocalDate.now(),
        lastDay = LocalDate.now(),
        dueDate = LocalDate.now().minusYears(correctionsPeriodsLimit - 1),
        submissionStatus = SubmissionStatus.Due,
        inProgress = true,
        isOldest = true
      )

      json.validate[Return] mustBe JsSuccess(dueReturn)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[Return] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "lastDay" -> "2025-01-17",
        "isOldest" -> true,
        "firstDay" -> "2025-01-17",
        "dueDate" -> "2023-01-17",
        "submissionStatus" -> 12345,
        "inProgress" -> true
      )

      json.validate[Return] mustBe a[JsError]
    }
  }
}
