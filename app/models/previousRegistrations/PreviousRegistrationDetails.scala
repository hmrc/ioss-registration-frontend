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

import models.Country
import models.domain._
import models.etmp.EtmpPreviousEuRegistrationDetails
import play.api.libs.json.{Json, OFormat}

case class PreviousRegistrationDetails(
                                        previousEuCountry: Country,
                                        previousSchemesDetails: Seq[PreviousSchemeDetails]
                                      )

object PreviousRegistrationDetails {

  def fromEtmpPreviousEuRegistrationDetails(country: Country,
                                            etmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]): PreviousRegistrationDetails = {

    val previousRegistrationDetails = etmpPreviousEuRegistrationDetails.collect {
      case registrationDetails: EtmpPreviousEuRegistrationDetails if registrationDetails.issuedBy == country.code =>
        PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registrationDetails)
    }

    PreviousRegistrationDetails(country, previousRegistrationDetails)
  }

  implicit val format: OFormat[PreviousRegistrationDetails] = Json.format[PreviousRegistrationDetails]
}