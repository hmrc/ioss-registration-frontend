/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.data.Forms.{text => playText}

import javax.inject.Inject

class WebsiteFormProvider @Inject() extends Mappings {

  def apply(thisIndex: Index, existingAnswers: Seq[String]): Form[Option[String]] =
    Form(
      "value" -> playText
        .transform[Option[String]](
          value => {
            val trimmed = value.trim
            if (trimmed.isEmpty) {
              None
            } else {
              val lower = trimmed.toLowerCase
              Some(
                if (lower.startsWith("http://") || lower.startsWith("https://")) trimmed else s"https://$trimmed"
              )
            }
          },
          _.getOrElse("")
        )
        .verifying(
          "website.error.length",
          _.forall(_.length <= 250)
        )
        .verifying(
          "website.error.duplicate",
          _.forall(site => !existingAnswers.zipWithIndex.exists {
            case (existing, i) => i != thisIndex.position && existing == site
          })
        )
        .verifying(
          "website.error.invalid",
          _.forall(site => site.matches(websitePattern))
        )
    )
}
