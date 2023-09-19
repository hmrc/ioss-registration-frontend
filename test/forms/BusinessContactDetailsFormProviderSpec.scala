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

import forms.validation.Validation.{commonTextPattern, emailPattern, telephonePattern}
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class BusinessContactDetailsFormProviderSpec extends StringFieldBehaviours {

  val formProvider = new BusinessContactDetailsFormProvider()
  val form = formProvider()

  ".fullName" - {

    val fieldName = "fullName"
    val requiredKey = "businessContactDetails.error.fullName.required"
    val lengthKey = "businessContactDetails.error.fullName.length"
    val invalidKey = "businessContactDetails.error.fullName.invalid"
    val validData = "Tom Smith"
    val maxLength = 100

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

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

    "must not bind invalid full name" in {
      val invalidFullName = "*@tom [smith]"
      val result = form.bind(Map(fieldName -> invalidFullName)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }

    "must not allow single double quotes, a curly apostrophe or a regular apostrophe at start of string but allow within the string" in {
      val newFullName = "’Tom O'Brian’"
      val result = form.bind(Map(fieldName -> newFullName)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }
  }

  ".telephoneNumber" - {

    val fieldName = "telephoneNumber"
    val requiredKey = "businessContactDetails.error.telephoneNumber.required"
    val lengthKey = "businessContactDetails.error.telephoneNumber.length"
    val invalidKey = "businessContactDetails.error.telephoneNumber.invalid"
    val maxLength = 20
    val validData = "0111 2223334"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    "must not bind invalid telephone data" in {
      val invalidTelephone = "invalid"
      val result = form.bind(Map(fieldName -> invalidTelephone)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(telephonePattern)))
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

  ".emailAddress" - {

    val fieldName = "emailAddress"
    val requiredKey = "businessContactDetails.error.emailAddress.required"
    val lengthKey = "businessContactDetails.error.emailAddress.length"
    val invalidKey = "businessContactDetails.error.emailAddress.invalid"
    val validData = "bar@example.com"
    val maxLength = 50

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    "must bind valid email address data with .co.uk" in {
      val validEmail = "test@email.co.uk"
      val result = form.bind(Map(fieldName -> validEmail)).apply(fieldName)
      result.value.value mustBe validEmail
      result.errors mustBe empty
    }

    "must not bind invalid email address data" in {
      val invalidEmail = "invalid"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with missing @" in {
      val invalidEmail = "email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with @@" in {
      val invalidEmail = "test@@email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with @." in {
      val invalidEmail = "test@.email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with missing ." in {
      val invalidEmail = "email@"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
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

}
