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

import forms.behaviours.BooleanFieldBehaviours
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

class ViewOrChangePreviousRegistrationFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey = "viewOrChangePreviousRegistration.error.required"
  private val invalidKey = "error.boolean"
  private val arbitraryIossNumber: String = arbitrary[String].sample.value

  private val form = new ViewOrChangePreviousRegistrationFormProvider()(arbitraryIossNumber)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, Seq(arbitraryIossNumber))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(arbitraryIossNumber))
    )
  }
}