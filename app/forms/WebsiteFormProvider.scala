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

import forms.mappings.Mappings
import forms.validation.Validation.websitePattern
import models.Index
import play.api.data.Form

import javax.inject.Inject

class WebsiteFormProvider @Inject() extends Mappings {

  def apply(thisIndex: Index, existingAnswers: Seq[String]): Form[String] =
    Form(
      "value" -> text("website.error.required")
        .verifying(firstError(
          maxLength(250, "website.error.length"),
          notADuplicate(thisIndex, existingAnswers, "website.error.duplicate"),
          regexp(websitePattern, "website.error.invalid")))
    )
}
