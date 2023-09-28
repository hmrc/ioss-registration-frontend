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

package models.previousRegistrations

import models.{Country, PreviousScheme}
import play.api.libs.json.{Json, OFormat}

case class PreviousRegistrationDetailsWithOptionalVatNumber(
                                                             previousEuCountry: Country,
                                                             previousSchemesDetails: Option[List[SchemeDetailsWithOptionalVatNumber]]
                                                           )


object PreviousRegistrationDetailsWithOptionalVatNumber {

  implicit val format: OFormat[PreviousRegistrationDetailsWithOptionalVatNumber] = Json.format[PreviousRegistrationDetailsWithOptionalVatNumber]
}

case class SchemeDetailsWithOptionalVatNumber(
                                               previousScheme: Option[PreviousScheme],
                                               previousSchemeNumbers: Option[SchemeNumbersWithOptionalVatNumber]
                                             )

object SchemeDetailsWithOptionalVatNumber {

  implicit val format: OFormat[SchemeDetailsWithOptionalVatNumber] = Json.format[SchemeDetailsWithOptionalVatNumber]
}

case class SchemeNumbersWithOptionalVatNumber(
                                               previousSchemeNumber: Option[String],
                                               previousIntermediaryNumber: Option[String]
                                             )

object SchemeNumbersWithOptionalVatNumber {

  implicit val format: OFormat[SchemeNumbersWithOptionalVatNumber] = Json.format[SchemeNumbersWithOptionalVatNumber]
}