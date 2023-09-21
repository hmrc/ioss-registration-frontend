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

package forms

import forms.behaviours.StringFieldBehaviours
import models.{Bic, Iban}
import play.api.data.FormError
import org.scalacheck.Arbitrary.arbitrary

class BankDetailsFormProviderSpec extends  StringFieldBehaviours {

  val form = new BankDetailsFormProvider()()

  ".accountName" - {

    val fieldName = "accountName"
    val invalid = "bankDetails.error.accountName.invalid"
    val requiredKey = "bankDetails.error.accountName.required"
    val lengthKey = "bankDetails.error.accountName.length"
    val maxLength = 70

    s"bind strings that do not contain forbidden characters" in {
      val result = form.bind(Map(fieldName -> "Abc 123 -?:().,'+")).apply(fieldName)
      result.errors mustBe empty
    }

    s"do not bind strings that contain forbidden characters" in {
      val result = form.bind(Map(fieldName -> "A & B")).apply(fieldName)
      result.errors must contain only FormError(fieldName, invalid, Seq("^[A-Za-z0-9\\/\\-?:().,'+ ]*$"))
    }

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".bic" - {

    val fieldName = "bic"
    val invalidKey = "bankDetails.error.bic.invalid"

    s"bind strings that match the BIC regular expression" in {

      forAll(arbitrary[Bic]) {
        validInput =>
          val result = form.bind(Map(fieldName -> validInput.toString)).apply(fieldName)
          result.errors mustBe empty
      }
    }

    "not bind any strings that don't match the BIC regular expression" in {

      val invalidCodes = Seq(
        "ABCDEF1A",
        "ABCDEF2O",
        "ABCDEF2AB",
        "ABCDEF2123",
        "ABCDE12A"
      )

      for (invalidCode <- invalidCodes) {
        val result = form.bind(Map(fieldName -> invalidCode)).apply(fieldName)
        result.errors must contain only FormError(fieldName, invalidKey)
      }
    }
  }

  ".iban" - {

    val fieldName = "iban"
    val requiredKey = "bankDetails.error.iban.required"
    val invalidKey = "bankDetails.error.iban.invalid"
    val checksumKey = "bankDetails.error.iban.checksum"

    val validData = arbitrary[Iban].map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must allow spaces in the input, but strip them when creating the resulting IBAN" in {

      val ibansWithSpaces = genIntersperseString(arbitrary[Iban].map(_.toString), " ")

      forAll(ibansWithSpaces) {
        input =>
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.value mustBe defined
          result.errors mustBe empty
      }
    }

    "must not bind values in the wrong format" in {

      forAll(arbitrary[String] suchThat (_.trim.nonEmpty)) {
        value =>

          whenever(Iban(value).isLeft) {
            val result = form.bind(Map(fieldName -> value)).apply(fieldName)
            result.errors mustEqual Seq(FormError(fieldName, invalidKey))
          }
      }
    }

    "must not bind values with an invalid checksum" in {

      forAll(arbitrary[Iban]) {
        iban =>
          val value = iban.toString.take(2) + "00" + iban.toString.substring(4)
          val result = form.bind(Map(fieldName -> value)).apply(fieldName)
          result.errors mustEqual Seq(FormError(fieldName, checksumKey))
      }
    }
  }
}
