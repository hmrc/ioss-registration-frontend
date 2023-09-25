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
import forms.validation.Validation.commonTextPattern
import models.Country
import play.api.data.FormError

class FixedEstablishmentTradingNameFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "fixedEstablishmentTradingName.error.required"
  private val lengthKey = "fixedEstablishmentTradingName.error.length"
  private val invalidKey = "fixedEstablishmentTradingName.error.invalid"
  private val validData = "Trading name"
  private val maxLength = 40

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  val form = new FixedEstablishmentTradingNameFormProvider()(country)

  ".value" - {

    val fieldName = "value"

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
      requiredError = FormError(fieldName, requiredKey, Seq(country.name))
    )

    "must not bind invalid Trading Name" in {
      val invalidFixedEstablishmentTradingName = "^Fixed est~ tr@ding=nam£"
      val result = form.bind(Map(fieldName -> invalidFixedEstablishmentTradingName)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }
  }
}
