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

package models.domain

import models.etmp.EtmpPreviousEuRegistrationDetails
import models.previousRegistrations.NonCompliantDetails
import models.{Country, PreviousScheme}
import play.api.libs.json.{Json, OFormat}

case class PreviousRegistration(country: Country, previousSchemesDetails: Seq[PreviousSchemeDetails])

object PreviousRegistration {

  def fromEtmpPreviousEuRegistrationDetailsByCountry(country: Country,
                                                     etmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]): PreviousRegistration = {
    val previousSchemeDetails = etmpPreviousEuRegistrationDetails.collect {
      case registrationDetails if registrationDetails.issuedBy == country.code =>
        PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registrationDetails)
    }

    PreviousRegistration(country, previousSchemeDetails)

  }

  def fromEtmpPreviousEuRegistrationDetails(allEtmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]): List[PreviousRegistration] = {
    val countrySchemaDetailsMapping: Map[Country, Seq[(Country, PreviousSchemeDetails)]] =
      allEtmpPreviousEuRegistrationDetails.map { etmpPreviousEuRegistrationDetails =>
        val country = Country.fromCountryCodeUnsafe(etmpPreviousEuRegistrationDetails.issuedBy)
        val details: PreviousSchemeDetails = PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(etmpPreviousEuRegistrationDetails)

        country -> details

      }.groupBy(_._1)

    countrySchemaDetailsMapping.map { case (country, countryPreviousSchemaDetails) =>
      PreviousRegistration(country = country, previousSchemesDetails = countryPreviousSchemaDetails.map(_._2))
    }.toList

  }


  implicit val format: OFormat[PreviousRegistration] = Json.format[PreviousRegistration]
}

case class PreviousSchemeDetails(
                                  previousScheme: PreviousScheme,
                                  previousSchemeNumbers: PreviousSchemeNumbers,
                                  nonCompliantDetails: Option[NonCompliantDetails]
                                )

object PreviousSchemeDetails {

  def fromEtmpPreviousEuRegistrationDetails(etmpPreviousEuRegistrationDetails: EtmpPreviousEuRegistrationDetails): PreviousSchemeDetails = {
    PreviousSchemeDetails(
      previousScheme = PreviousScheme.fromEmtpSchemaType(etmpPreviousEuRegistrationDetails.schemeType),
      previousSchemeNumbers = PreviousSchemeNumbers(
        previousSchemeNumber = etmpPreviousEuRegistrationDetails.registrationNumber,
        previousIntermediaryNumber = etmpPreviousEuRegistrationDetails.intermediaryNumber,
      ),
      None
    )
  }

  implicit val format: OFormat[PreviousSchemeDetails] = Json.format[PreviousSchemeDetails]
}
