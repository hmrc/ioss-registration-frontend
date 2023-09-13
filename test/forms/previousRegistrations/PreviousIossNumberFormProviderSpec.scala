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

package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import models.Country
import play.api.data.FormError

class PreviousIossNumberFormProviderSpec extends OptionFieldBehaviours {

  private val country = Country.euCountries.head

  ".previousSchemeNumber" - {

    val requiredKey = "previousIossNumber.error.schemeNumber.required"

    val fieldName = "previousSchemeNumber"

    val form = new PreviousIossNumberFormProvider()(country, false)

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".previousIntermediaryNumber as optional field" - {

    val requiredKey = "previousIntermediaryNumber.error.invalid"

    val fieldName = "previousIntermediaryNumber"

    val form = new PreviousIossNumberFormProvider()(country, false)

    behave like optionsField(
      form,
      fieldName,
      Seq("IN0401234567"),
      invalidError = FormError(fieldName, requiredKey)
    )
  }

  ".previousIntermediaryNumber as mandatory field" - {

    val requiredKey = "previousIossNumber.error.intermediaryNumber.required"

    val fieldName = "previousIntermediaryNumber"

    val form = new PreviousIossNumberFormProvider()(country, true)

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
