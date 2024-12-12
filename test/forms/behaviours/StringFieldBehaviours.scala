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

package forms.behaviours

import play.api.data.{Form, FormError}

trait StringFieldBehaviours extends FieldBehaviours {

  def fieldWithMaxLength(form: Form[_],
                         fieldName: String,
                         maxLength: Int,
                         lengthError: FormError): Unit = {

    s"not bind strings longer than $maxLength characters" in {

      forAll(stringsLongerThan(maxLength) -> "longString") {
        string =>
          val result = form.bind(Map(fieldName -> string)).apply(fieldName)
          result.errors must contain only lengthError
      }
    }
  }

  def fieldWithMinLength(form: Form[_],
                         fieldName: String,
                         minLength: Int,
                         lengthError: FormError): Unit = {

    s"not bind strings shorter than $minLength characters" in {

      forAll(stringsShorterThan(minLength) -> "shortString") {
        string =>
          val result = form.bind(Map(fieldName -> string)).apply(fieldName)
          result.errors must contain only lengthError
      }
    }
  }

  def commonTextField(form: Form[_],
                      fieldName: String,
                      formatError: FormError,
                      lengthError: FormError,
                      maxLength: Int): Unit = {

    s"not bind strings longer than $maxLength characters" in {

      forAll(stringsLongerThan(maxLength) -> "longString") {
        string =>
          val result = form.bind(Map(fieldName -> string)).apply(fieldName)
          result.errors must contain(lengthError)
      }
    }

    "not bind incorrect values" in {
      forAll(unsafeInputsWithMaxLength(maxLength)) {
        (invalidInput: String) =>
          val result = form.bind(Map(fieldName -> invalidInput)).apply(fieldName)
          result.errors must contain(formatError)
      }
    }

    "bind correct values" in {
      forAll(commonFieldString(maxLength)) {
        (validInput: String) =>
          val result = form.bind(Map(fieldName -> validInput)).apply(fieldName)
          result.errors mustBe empty
      }
    }
  }
}
