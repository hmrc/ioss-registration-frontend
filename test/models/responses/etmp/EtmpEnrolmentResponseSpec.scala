/*
 * Copyright 2025 HM Revenue & Customs
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

package models.responses.etmp

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}

class EtmpEnrolmentResponseSpec extends AnyFreeSpec with Matchers {

  "EtmpEnrolmentResponse" - {

    "serialize to JSON correctly" in {
      val enrolmentResponse = EtmpEnrolmentResponse("test-ioss-reference")
      val expectedJson = Json.obj("iossReference" -> "test-ioss-reference")

      val json = Json.toJson(enrolmentResponse)
      json mustBe expectedJson
    }

    "deserialize from JSON correctly" in {

      val json = Json.obj("iossReference" -> "test-ioss-reference")
      val expectedEnrolmentResponse = EtmpEnrolmentResponse("test-ioss-reference")
      
      json.as[EtmpEnrolmentResponse] mustBe expectedEnrolmentResponse
    }

    "fail deserialization when required fields are missing" in {
      val invalidJson = Json.obj()
      
      val result = invalidJson.validate[EtmpEnrolmentResponse]
      result.isError mustBe true
    }

    "fail deserialization when field types are incorrect" in {
      val invalidJson = Json.obj("iossRefrence" -> 12345)

      val result = invalidJson.validate[EtmpEnrolmentResponse]
      result.isError mustBe true
    }
  }
}

