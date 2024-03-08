/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.amend

import forms.behaviours.StringFieldBehaviours
import models.amend.PreviousRegistration
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class ViewOrChangePreviousRegistrationsMultipleFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "viewOrChangePreviousRegistrationsMultiple.error.required"
  private val invalidKey = "viewOrChangePreviousRegistrationsMultiple.error.invalid"

  private val previousRegistrations: Seq[PreviousRegistration] = Gen.listOf(arbitraryPreviousRegistration.arbitrary).sample.value
  private val form: Form[String] = new ViewOrChangePreviousRegistrationsMultipleFormProvider()(previousRegistrations)
  private val validAnswer: String = previousRegistrations.map(_.iossNumber).head
  private val invalidAnswer: String = arbitraryPreviousRegistration.arbitrary
    .suchThat(_.iossNumber.toSeq != previousRegistrations.map(_.iossNumber)).sample.value.iossNumber

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validAnswer
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind invalid IOSS number" in {
      val invalidIossNumber = invalidAnswer
      val result = form.bind(Map(fieldName -> invalidIossNumber)).apply(fieldName)
      result.errors must contain only FormError(fieldName, invalidKey)
    }
  }
}
