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

package models.responses

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsNull, JsValue, Json}

import java.time.Instant
import java.util.UUID

class CoreErrorResponseSpec extends AnyFreeSpec with Matchers {

  "CoreErrorResponse" - {

    "must serialize to JSON correctly" in {
      val errorResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      val expectedJson = Json.obj(
        "timestamp" -> "2023-01-01T00:00:00Z",
        "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
        "errorCode" -> "OSS_001",
        "errorMessage" -> "An error occurred"
      )

      Json.toJson(errorResponse) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "timestamp" -> "2023-01-01T00:00:00Z",
        "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
        "errorCode" -> "OSS_001",
        "errorMessage" -> "An error occurred"
      )

      val expectedResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      json.as[CoreErrorResponse] mustBe expectedResponse
    }

    "must handle missing optional fields during deserialization" in {
      val json = Json.obj(
        "timestamp" -> "2023-01-01T00:00:00Z",
        "errorCode" -> "OSS_001",
        "errorMessage" -> "An error occurred"
      )

      val expectedResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = None,
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      json.as[CoreErrorResponse] mustBe expectedResponse
    }

    "must fail to deserialize when required fields are missing" in {
      val invalidJson = Json.obj(
        "errorDetail" -> Json.obj(
          "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
          "errorCode" -> "OSS_001"
        )
      )

      val result = invalidJson.validate[EisErrorResponse]
      result.isError mustBe true
    }

    "must fail to deserialize when field types are invalid" in {
      val invalidJson = Json.obj(
        "errorDetail" -> Json.obj(
          "timestamp" -> "Invalid timestamp",
          "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
          "errorCode" -> 123,
          "errorMessage" -> true
        )
      )

      val result = invalidJson.validate[EisErrorResponse]
      result.isError mustBe true
    }

    "must generate a proper exception message from asException" in {
      val errorResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      val exception = errorResponse.asException
      exception.getMessage mustBe "2023-01-01T00:00:00Z Some(123e4567-e89b-12d3-a456-426614174000) OSS_001 An error occurred"
    }

    "must serialize and deserialize correctly (round-trip test)" in {
      val errorResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      val json = Json.toJson(errorResponse)
      json.as[CoreErrorResponse] mustBe errorResponse
    }

    "must handle null transactionId during deserialization" in {
      val json = Json.obj(
        "timestamp" -> "2023-01-01T00:00:00Z",
        "transactionId" -> JsNull,
        "errorCode" -> "OSS_001",
        "errorMessage" -> "An error occurred"
      )

      val expectedResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = None,
        errorCode = "OSS_001",
        errorMessage = "An error occurred"
      )

      json.as[CoreErrorResponse] mustBe expectedResponse
    }

    "must handle empty errorMessage" in {
      val errorResponse = CoreErrorResponse(
        timestamp = Instant.parse("2023-01-01T00:00:00Z"),
        transactionId = Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        errorCode = "OSS_001",
        errorMessage = ""
      )

      val json = Json.obj(
        "timestamp" -> "2023-01-01T00:00:00Z",
        "transactionId" -> "123e4567-e89b-12d3-a456-426614174000",
        "errorCode" -> "OSS_001",
        "errorMessage" -> ""
      )

      Json.toJson(errorResponse) mustBe json
      json.as[CoreErrorResponse] mustBe errorResponse
    }

    "must have the correct value for REGISTRATION_NOT_FOUND" in {
      CoreErrorResponse.REGISTRATION_NOT_FOUND mustBe "OSS_009"
    }
  }
}

