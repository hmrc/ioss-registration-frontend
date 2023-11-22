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

import forms.mappings.{IntermediaryIdentificationNumberConstraints, IossRegistrationNumberConstraints, Mappings}
import models.Country
import models.domain.PreviousSchemeNumbers
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import uk.gov.voa.play.form.ConditionalMappings.mandatory

import javax.inject.Inject

class PreviousIossNumberFormProvider @Inject() extends Mappings with IossRegistrationNumberConstraints
  with IntermediaryIdentificationNumberConstraints{

  def apply(country: Country, hasIntermediary: Boolean): Form[PreviousSchemeNumbers] =
    Form(
      mapping(
        "previousSchemeNumber" -> text("previousIossNumber.error.schemeNumber.required")
          .transform[String](_.trim.replaceAll("\\s", "").toUpperCase, value => value)
          .verifying(validateIossRegistrationNumber(country.code, "previousIossNumber.error.invalid")),
        if (hasIntermediary) {
          "previousIntermediaryNumber" -> mandatory(text("previousIossNumber.error.intermediaryNumber.required")
            .transform[String](_.trim.replaceAll("\\s", "").toUpperCase, value => value)
            .verifying(validateIntermediaryIdentificationNumber(country.code, "previousIntermediaryNumber.error.invalid")))
        } else {
          "previousIntermediaryNumber" -> optional(text("previousIossNumber.error.intermediaryNumber.required")
            .transform[String](_.trim.replaceAll("\\s", "").toUpperCase, value => value)
            .verifying(validateIntermediaryIdentificationNumber(country.code, "previousIntermediaryNumber.error.invalid")))
        }
      )(PreviousSchemeNumbers.apply)(PreviousSchemeNumbers.unapply)
    )
}
