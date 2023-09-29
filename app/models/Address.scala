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

package models

import models.domain.ModelHelpers.normaliseSpaces
import play.api.libs.json._

sealed trait Address

object Address {

  def reads: Reads[Address] =
    DesAddress.format.widen[Address] orElse
    InternationalAddress.format.widen[Address]


  def writes: Writes[Address] = Writes {
    case d: DesAddress => Json.toJson(d)(DesAddress.format)
    case i: InternationalAddress => Json.toJson(i)(InternationalAddress.format)
  }

  implicit def format: Format[Address] = Format(reads, writes)
}

case class DesAddress(
                       line1: String,
                       line2: Option[String],
                       line3: Option[String],
                       line4: Option[String],
                       line5: Option[String],
                       postCode: Option[String],
                       countryCode: String
                     ) extends Address

object DesAddress {

  implicit val format: OFormat[DesAddress] = Json.format[DesAddress]

  def apply(
             line1: String,
             line2: Option[String],
             line3: Option[String],
             line4: Option[String],
             line5: Option[String],
             postCode: Option[String],
             countryCode: String
           ): DesAddress = new DesAddress(
    normaliseSpaces(line1),
    normaliseSpaces(line2),
    normaliseSpaces(line3),
    normaliseSpaces(line4),
    normaliseSpaces(line5),
    normaliseSpaces(postCode),
    countryCode
  )
}

case class InternationalAddress(line1: String,
                                line2: Option[String],
                                townOrCity: String,
                                stateOrRegion: Option[String],
                                postCode: Option[String],
                                country: Country
                               ) extends Address

object InternationalAddress {

  implicit val format: OFormat[InternationalAddress] = Json.format[InternationalAddress]

  def apply(line1: String,
            line2: Option[String],
            townOrCity: String,
            stateOrRegion: Option[String],
            postCode: Option[String],
            country: Country): InternationalAddress = new InternationalAddress(normaliseSpaces(line1),
    normaliseSpaces(line2),
    normaliseSpaces(townOrCity),
    normaliseSpaces(stateOrRegion),
    normaliseSpaces(postCode),
    country)
}
