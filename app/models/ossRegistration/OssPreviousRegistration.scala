/*
 * Copyright 2024 HM Revenue & Customs
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

package models.ossRegistration

import models.Country
import play.api.libs.json.{Json, OFormat, Reads, Writes}

sealed trait OssPreviousRegistration

object OssPreviousRegistration {

  implicit val reads: Reads[OssPreviousRegistration] =
    OssPreviousRegistrationNew.format.widen[OssPreviousRegistration] orElse
      OssPreviousRegistrationLegacy.format.widen[OssPreviousRegistration]

  implicit val writes: Writes[OssPreviousRegistration] = Writes {
    case p: OssPreviousRegistrationNew => Json.toJson(p)(OssPreviousRegistrationNew.format)
    case l: OssPreviousRegistrationLegacy => Json.toJson(l)(OssPreviousRegistrationLegacy.format)
  }
}

case class OssPreviousRegistrationNew(
                                    country: Country,
                                    previousSchemesDetails: Seq[OssPreviousSchemeDetails]
                                  ) extends OssPreviousRegistration

object OssPreviousRegistrationNew {

  implicit val format: OFormat[OssPreviousRegistrationNew] = Json.format[OssPreviousRegistrationNew]
}

case class OssPreviousRegistrationLegacy(
                                       country: Country,
                                       vatNumber: String
                                     ) extends OssPreviousRegistration

object OssPreviousRegistrationLegacy {

  implicit val format: OFormat[OssPreviousRegistrationLegacy] = Json.format[OssPreviousRegistrationLegacy]
}

case class OssPreviousSchemeDetails(previousScheme: OssPreviousScheme, previousSchemeNumbers: OssPreviousSchemeNumbers)

object OssPreviousSchemeDetails {

  implicit val format: OFormat[OssPreviousSchemeDetails] = Json.format[OssPreviousSchemeDetails]
}


case class OssPreviousSchemeNumbers(
                                  previousSchemeNumber: String,
                                  previousIntermediaryNumber: Option[String]
                                )

object OssPreviousSchemeNumbers {

  implicit val format: OFormat[OssPreviousSchemeNumbers] = Json.format[OssPreviousSchemeNumbers]
}
