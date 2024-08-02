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
import models.BankDetails
import play.api.data.Form
import play.api.data.Forms._
import validation.Validation

import javax.inject.Inject

class BankDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[BankDetails] = Form(
    mapping(
      "accountName" -> text("bankDetails.error.accountName.required")
        .transform[String](_.trim.replaceAll("\\s{2,}", " "), identity)
        .verifying(firstError(
          maxLength(70, "bankDetails.error.accountName.length"),
          regexp(Validation.bankAccountNamePattern, "bankDetails.error.accountName.invalid")
        )),
      "bic"  -> optional(bic("bankDetails.error.bic.required", "bankDetails.error.bic.invalid")),
      "iban" -> iban("bankDetails.error.iban.required", "bankDetails.error.iban.invalid", "bankDetails.error.iban.checksum")
    )(BankDetails.apply)(BankDetails.unapply)
  )
}
