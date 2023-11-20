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

import forms.mappings.{EuVatNumberConstraints, Mappings, PreviousRegistrationSchemeConstraint}
import models.{Country, PreviousScheme}
import play.api.data.Form

import javax.inject.Inject

class PreviousOssNumberFormProvider @Inject() extends Mappings with EuVatNumberConstraints with PreviousRegistrationSchemeConstraint{

  def apply(country: Country, previousSchemes: Seq[PreviousScheme]): Form[String] =
    Form(
      "value" -> text("previousOssNumber.error.required", Seq(country.name))
        .transform[String](_.trim.replaceAll("\\s", "").toUpperCase, value => value)
        .verifying(
          validateEuVatNumberOrEu(country.code, "previousOssNumber.error.invalid")
        )
        .transform[String](_.toUpperCase, value => value)
        .verifying(
          validatePreviousOssScheme(country, previousSchemes, "previousScheme.oss.schemes.exceed.error")
        )
    )
}
