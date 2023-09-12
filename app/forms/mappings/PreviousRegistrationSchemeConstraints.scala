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

package forms.mappings

import config.Constants.{maxIossSchemes, maxOssSchemes}
import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import play.api.data.validation.{Constraint, Invalid, Valid}

trait PreviousRegistrationSchemeConstraint {

  def validatePreviousRegistrationSchemes(countryName: String, existingAnswers: Seq[PreviousScheme],
                                          errorKeyOss: String, errorKeyIoss: String, schemeIndex: Index): Constraint[PreviousSchemeType] = {

    Constraint {
      input =>
        input match {
          case PreviousSchemeType.OSS if isEditingOrLessThanAllowedAmount(
            existingAnswers = existingAnswers,
            schemeIndex = schemeIndex,
            allowedSchemes = Seq(PreviousScheme.OSSU, PreviousScheme.OSSNU),
            amountOfSchemesAllowed = maxOssSchemes
          ) =>
            Invalid(errorKeyOss, countryName)
          case PreviousSchemeType.IOSS if isEditingOrLessThanAllowedAmount(
            existingAnswers = existingAnswers,
            schemeIndex = schemeIndex,
            allowedSchemes = Seq(PreviousScheme.IOSSWI, PreviousScheme.IOSSWOI),
            amountOfSchemesAllowed = maxIossSchemes
          ) =>
            Invalid(errorKeyIoss, countryName)
          case _ => Valid
        }
    }
  }

  private def isEditingOrLessThanAllowedAmount(existingAnswers: Seq[PreviousScheme],
                                               schemeIndex: Index,
                                               allowedSchemes: Seq[PreviousScheme],
                                               amountOfSchemesAllowed: Int): Boolean = {
    val currentOssCount = existingAnswers.count(value => allowedSchemes.contains(value))

    if (schemeIndex.position < currentOssCount) {
      false
    } else {
      currentOssCount >= amountOfSchemesAllowed
    }
  }

  def validatePreviousOssScheme(country: Country, existingAnswers: Seq[PreviousScheme], errorKeyOss: String): Constraint[String] = {
    Constraint {
      input =>
        input match {
          case string if string.startsWith("EU") && existingAnswers.contains(PreviousScheme.OSSNU) =>
            Invalid(errorKeyOss, "non-union", country.name)
          case string if string.startsWith(country.code) && existingAnswers.contains(PreviousScheme.OSSU) =>
            Invalid(errorKeyOss, "union", country.name)
          case _ =>
            Valid
        }
    }
  }
}