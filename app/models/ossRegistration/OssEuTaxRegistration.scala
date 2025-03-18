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

sealed trait OssEuTaxRegistration {
  val country: Country
}

object OssEuTaxRegistration {

  implicit val reads: Reads[OssEuTaxRegistration] =
    OssRegistrationWithFixedEstablishment.format.widen[OssEuTaxRegistration] orElse
      OssRegistrationWithoutFixedEstablishmentWithTradeDetails.format.widen[OssEuTaxRegistration] orElse
      OssRegistrationWithoutFixedEstablishment.format.widen[OssEuTaxRegistration] orElse
      OssEuVatRegistration.format.widen[OssEuTaxRegistration] orElse
      OssRegistrationWithoutTaxId.format.widen[OssEuTaxRegistration]


  implicit val writes: Writes[OssEuTaxRegistration] = Writes {
    case v: OssEuVatRegistration                     => Json.toJson(v)(OssEuVatRegistration.format)
    case fe: OssRegistrationWithFixedEstablishment   => Json.toJson(fe)(OssRegistrationWithFixedEstablishment.format)
    case fe: OssRegistrationWithoutFixedEstablishmentWithTradeDetails   => Json.toJson(fe)(OssRegistrationWithoutFixedEstablishmentWithTradeDetails.format)
    case fe: OssRegistrationWithoutFixedEstablishment   => Json.toJson(fe)(OssRegistrationWithoutFixedEstablishment.format)
    case w: OssRegistrationWithoutTaxId => Json.toJson(w)(OssRegistrationWithoutTaxId.format)
  }
}

final case class OssEuVatRegistration(
                                    country: Country,
                                    vatNumber: String
                                  ) extends OssEuTaxRegistration

object OssEuVatRegistration {

  implicit val format: OFormat[OssEuVatRegistration] =
    Json.format[OssEuVatRegistration]
}

final case class OssRegistrationWithFixedEstablishment(
                                                     country: Country,
                                                     taxIdentifier: OssEuTaxIdentifier,
                                                     fixedEstablishment: OssTradeDetails
                                                   ) extends OssEuTaxRegistration

object OssRegistrationWithFixedEstablishment {
  implicit val format: OFormat[OssRegistrationWithFixedEstablishment] =
    Json.format[OssRegistrationWithFixedEstablishment]
}


final case class OssRegistrationWithoutFixedEstablishmentWithTradeDetails(
                                           country: Country,
                                           taxIdentifier: OssEuTaxIdentifier,
                                           tradeDetails: OssTradeDetails
                                         ) extends OssEuTaxRegistration

object OssRegistrationWithoutFixedEstablishmentWithTradeDetails {

  implicit val format: OFormat[OssRegistrationWithoutFixedEstablishmentWithTradeDetails] =
    Json.format[OssRegistrationWithoutFixedEstablishmentWithTradeDetails]
}


final case class OssRegistrationWithoutFixedEstablishment(
                                                        country: Country,
                                                        taxIdentifier: OssEuTaxIdentifier,
                                                      ) extends OssEuTaxRegistration

object OssRegistrationWithoutFixedEstablishment {

  implicit val format: OFormat[OssRegistrationWithoutFixedEstablishment] =
    Json.format[OssRegistrationWithoutFixedEstablishment]
}

final case class OssRegistrationWithoutTaxId(country: Country) extends OssEuTaxRegistration

object OssRegistrationWithoutTaxId {
  implicit val format: OFormat[OssRegistrationWithoutTaxId] =
    Json.format[OssRegistrationWithoutTaxId]
}
