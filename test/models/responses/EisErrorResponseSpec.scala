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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

class EisErrorResponseSpec extends AnyFreeSpec with Matchers {

  "EisErrorResponse" - {

    "serialize to JSON correctly" in {

      val errorResponse = EisErrorResponse(
        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
        error = "SomeError",
        errorMessage = "An error occurred"
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "error": "SomeError",
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      Json.toJson(errorResponse) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "error": "SomeError",
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      val expectedResponse = EisErrorResponse(
        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
        error = "SomeError",
        errorMessage = "An error occurred"
      )

      json.as[EisErrorResponse] mustBe expectedResponse
    }

    "fail deserialization when required fields are missing" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisErrorResponse]
      result.isError mustBe true
    }

    "fail deserialization when field types are incorrect" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "error": 1,
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisErrorResponse]
      result.isError mustBe true
    }
  }

  "EisDisplayErrorResponse" - {

    "serialize to JSON correctly" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val displayErrorResponse = EisDisplayErrorResponse(errorDetail)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "12345",
          |    "errorCode": "089",
          |    "errorMessage": "No registration found",
          |    "timestamp": "2023-01-01T12:00:00Z"
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(displayErrorResponse) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "12345",
          |    "errorCode": "089",
          |    "errorMessage": "No registration found",
          |    "timestamp": "2023-01-01T12:00:00Z"
          |  }
          |}
          |""".stripMargin
      )

      val expectedErrorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val expectedResponse = EisDisplayErrorResponse(expectedErrorDetail)

      json.as[EisDisplayErrorResponse] mustBe expectedResponse
    }

    "fail deserialization when required fields are missing" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisDisplayErrorResponse]
      result.isError mustBe true
    }

    "fail deserialization when field types are incorrect" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "correlationId": 12345,
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisDisplayErrorResponse]
      result.isError mustBe true
    }
  }

  "EisDisplayErrorDetail" - {

    "serialize to JSON correctly" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "correlationId": "12345",
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      Json.toJson(errorDetail) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "correlationId": "12345",
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val expectedErrorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      json.as[EisDisplayErrorDetail] mustBe expectedErrorDetail
    }

    "fail deserialization when required fields are missing" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisDisplayErrorDetail]
      result.isError mustBe true
    }

    "fail deserialization when field types are incorrect" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "correlationId": 12345,
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val result = json.validate[EisDisplayErrorDetail]
      result.isError mustBe true
    }
    
    "have the correct display error code constant" in {
      EisDisplayErrorDetail.displayErrorCodeNoRegistration mustBe "089"
    }
  }

  "EisDisplayErrorResponse companion object" - {

    "have the correct display error code constant" in {
      EisDisplayErrorResponse.displayErrorCodeNoRegistration mustBe "089"
    }
  }
}

