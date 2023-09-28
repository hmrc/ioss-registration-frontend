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

import models.CountryWithValidationDetails
import play.api.data.validation.{Constraint, Invalid, Valid}


trait EuVatNumberConstraints {

  private def getCountryVatRegex(countryCode: String): String =
    CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode)
    match {
      case Some(countryWithValidationDetails) =>
        countryWithValidationDetails.vrnRegex
      case _ => throw new Exception("invalid country code")
    }

  def validateEuVatNumber(countryCode: String, errorKey: String): Constraint[String] = {

    Constraint {
      input =>
        if (matchesCountryRegex(input, countryCode)) {
          Valid
        } else {
          Invalid(errorKey)
        }

    }
  }

  private def matchesCountryRegex(input: String, countryCode: String): Boolean = {
    val regex = getCountryVatRegex(countryCode)
    input.matches(regex)
  }

  def validateEuVatNumberOrEu(countryCode: String, errorKey: String): Constraint[String] = {
    Constraint {
      input =>
        if (checkEuRegistrationNumber(input)) {
          Valid
        } else if (matchesCountryRegex(input, countryCode)) {
          Valid
        } else {
          Invalid(errorKey)
        }
    }
  }

  private def checkEuRegistrationNumber(input: String): Boolean = {
    val regex = """^(?i)EU[0-9]{9}$"""
    input.matches(regex)
  }
}
