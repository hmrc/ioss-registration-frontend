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
import forms.validation.Validation.websitePattern
import models.Index
import play.api.data.FormError

class WebsiteFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "website.error.required"
  val lengthKey = "website.error.length"
  val invalidKey = "website.error.invalid"
  val maxLength = 250
  val validData = "https://www.validwebsite.com"
  val validData2 = "https://validwebsite.com"
  val index = Index(0)
  val emptyExistingAnswers = Seq.empty[String]

  val formProvider: WebsiteFormProvider = new WebsiteFormProvider()
  val form = formProvider(index, emptyExistingAnswers)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    "bind valid website without www prefix" in {
      val result = form.bind(Map(fieldName -> validData2)).apply(fieldName)
      result.value.value mustBe validData2
      result.errors mustBe empty
    }

    "must not bind invalid website data" in {
      val invalidWebsite = "invalid"
      val result = form.bind(Map(fieldName -> invalidWebsite)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(websitePattern)))
    }

    "must not bind invalid website data with missing ." in {
      val invalidWebsite = "https://www.websitecom"
      val result = form.bind(Map(fieldName -> invalidWebsite)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(websitePattern)))
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

    "must fail to bind when given a duplicate value" in {
      val existingAnswers = Seq("https://foo", "https://bar")
      val answer = "https://bar"
      val form = new WebsiteFormProvider()(index, existingAnswers)

      val result = form.bind(Map(fieldName -> answer)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "website.error.duplicate")
    }
  }
}
