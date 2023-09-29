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

package forms.euDetails

import forms.behaviours.StringFieldBehaviours
import forms.validation.Validation.alphaNumericWithSpace
import models.Country
import play.api.data.FormError

class EuTaxReferenceFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "euTaxReference.error.required"
  private val lengthKey = "euTaxReference.error.length"
  private val formatKey = "euTaxReference.error.format"
  private val maxLength = 20
  private val minLength = 1

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  val form = new EuTaxReferenceFormProvider()(country)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(country.name))
    )

    s"not bind strings longer than $maxLength characters" in {

      forAll(stringsLongerThan(maxLength) -> "longString") {
        string =>
          val result = form.bind(Map(fieldName -> string)).apply(fieldName)
          result.errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
      }
    }

    "not bind incorrect values" in {
      forAll(unsafeInputsWithMaxLength(maxLength)) {
        invalidInput: String =>
          val result = form.bind(Map(fieldName -> invalidInput)).apply(fieldName)
          result.errors must contain(FormError(fieldName, formatKey, Seq(alphaNumericWithSpace)))
      }
    }

    "bind correct values" in {
      forAll(alphaNumStringWithLength(minLength, maxLength - 1)) {
        validInput: String =>
          val result = form.bind(Map(fieldName -> validInput)).apply(fieldName + " ")
          result.errors mustBe empty
      }
    }
  }
}
