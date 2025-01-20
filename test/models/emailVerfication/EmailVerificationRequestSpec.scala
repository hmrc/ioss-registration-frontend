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
import models.emailVerification.{EmailVerificationRequest, VerifyEmail}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}


class EmailVerificationRequestSpec extends AnyFreeSpec with Matchers with SpecBase {

  "EmailVerificationRequest" - {

    "must serialise and deserialise to and from a EmailVerificationRequest" - {

      "with all optional fields present" in {

        val emailVerificationRequest: EmailVerificationRequest =
          EmailVerificationRequest(
            "12345-credId",
            "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-account-details",
            "OSS",
            Some("one-stop-shop-registration-frontend"),
            "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
            Some("Register to pay VAT on distance sales of goods from Northern Ireland to the EU"),
            Some("/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"),
            Some(VerifyEmail(
              "email@example.com",
              "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
            ))
          )

        val expectedJson = Json.obj(
          "credId" -> "12345-credId",
          "continueUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-account-details",
          "origin" -> "OSS",
          "deskproServiceName" -> "one-stop-shop-registration-frontend",
          "accessibilityStatementUrl" -> "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
          "pageTitle" -> "Register to pay VAT on distance sales of goods from Northern Ireland to the EU",
          "backUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details",
          "email" -> Json.obj(
            "address" -> "email@example.com",
            "enterUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
          ),
          "lang" -> "en"
        )

        Json.toJson(emailVerificationRequest) mustEqual expectedJson
        expectedJson.validate[EmailVerificationRequest] mustEqual JsSuccess(emailVerificationRequest)
      }

      "with all optional fields missing" in {
        val emailVerificationRequest: EmailVerificationRequest =
          EmailVerificationRequest(
            "12345-credId",
            "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-account-details",
            "OSS",
            None,
            "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
            None,
            None,
            None
          )

        val expectedJson = Json.obj(
          "credId" -> "12345-credId",
          "continueUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-account-details",
          "origin" -> "OSS",
          "accessibilityStatementUrl" -> "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
          "lang" -> "en"
        )

        Json.toJson(emailVerificationRequest) mustEqual expectedJson
        expectedJson.validate[EmailVerificationRequest] mustEqual JsSuccess(emailVerificationRequest)
      }


      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[EmailVerificationRequest] mustBe a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "credId" -> 12345,
          "continueUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/bank-account-details",
          "origin" -> "OSS",
          "accessibilityStatementUrl" -> "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
          "lang" -> "en"
        )

        expectedJson.validate[EmailVerificationRequest] mustBe a[JsError]
      }
    }
  }

  "verifyEmail" - {

    "must serialize to JSON correctly" in {
      val verifyEmail = VerifyEmail(
        "email@example.com",
        "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
      )

      val expectedJson = Json.obj(
        "address" -> "email@example.com",
        "enterUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
      )

      Json.toJson(verifyEmail) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val expectedJson = Json.obj(
        "address" -> "email@example.com",
        "enterUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
      )

      val verifyEmail = VerifyEmail(
        "email@example.com",
        "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
      )

      expectedJson.validate[VerifyEmail] mustBe JsSuccess(verifyEmail)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[VerifyEmail] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "address" -> 12345,
        "enterUrl" -> "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
      )

      expectedJson.validate[VerifyEmail] mustBe a[JsError]
    }
  }

}
