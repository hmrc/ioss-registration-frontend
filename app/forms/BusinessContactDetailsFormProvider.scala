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

import forms.Validation.Validation.{commonTextPattern, emailPattern, telephonePattern}

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.BusinessContactDetails

class BusinessContactDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[BusinessContactDetails] = Form(
     mapping(
      "fullName" -> text("businessContactDetails.error.fullName.required")
        .verifying(firstError(
          maxLength(100, "businessContactDetails.error.fullName.length"),
          regexp(commonTextPattern, "businessContactDetails.error.fullName.invalid"))),
      "telephoneNumber" -> text("businessContactDetails.error.telephoneNumber.required")
        .verifying(firstError(
          maxLength(20, "businessContactDetails.error.telephoneNumber.length"),
          regexp(telephonePattern, "businessContactDetails.error.telephoneNumber.invalid"))),
       "emailAddress" -> text("businessContactDetails.error.emailAddress.required")
         .verifying(firstError(
           maxLength(50, "businessContactDetails.error.emailAddress.length"),
           regexp(emailPattern, "businessContactDetails.error.emailAddress.invalid")))
    )(BusinessContactDetails.apply)(BusinessContactDetails.unapply)
   )
 }
