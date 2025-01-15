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
    DesAddress.reads.widen[Address] orElse
    InternationalAddress.reads.widen[Address]


  def writes: Writes[Address] = Writes {
    case d: DesAddress => Json.toJson(d)(DesAddress.writes)
    case i: InternationalAddress => Json.toJson(i)(InternationalAddress.writes)
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

  implicit val reads: Reads[DesAddress] = {

    import play.api.libs.functional.syntax._
    (
      (__ \ "line1").read[String].map(normaliseSpaces) and
        (__ \ "line2").readNullable[String].map(normaliseSpaces) and
        (__ \ "line3").readNullable[String].map(normaliseSpaces) and
        (__ \ "line4").readNullable[String].map(normaliseSpaces) and
        (__ \ "line5").readNullable[String].map(normaliseSpaces) and
        (__ \ "postCode").readNullable[String].map(normaliseSpaces) and
        (__ \ "countryCode").read[String]
      )(DesAddress(_, _, _, _, _, _, _))
  }

  implicit val writes: OWrites[DesAddress] = (o: DesAddress) => {
    val line2Obj = o.line2.map(x => Json.obj("line2" -> x)).getOrElse(Json.obj())
    val line3Obj = o.line3.map(x => Json.obj("line3" -> x)).getOrElse(Json.obj())
    val line4Obj = o.line4.map(x => Json.obj("line4" -> x)).getOrElse(Json.obj())
    val line5Obj = o.line5.map(x => Json.obj("line5" -> x)).getOrElse(Json.obj())
    val postCodeObj = o.postCode.map(x => Json.obj("postCode" -> x)).getOrElse(Json.obj())

    Json.obj(
      "line1" -> o.line1,
      "countryCode" -> o.countryCode
    ) ++ line2Obj ++ line3Obj ++ line4Obj ++ line5Obj ++ postCodeObj
  }

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

  implicit val reads: Reads[InternationalAddress] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "line1").read[String].map(normaliseSpaces) and
        (__ \ "line2").readNullable[String].map(normaliseSpaces) and
        (__ \ "townOrCity").read[String].map(normaliseSpaces) and
        (__ \ "stateOrRegion").readNullable[String].map(normaliseSpaces) and
        (__ \ "postCode").readNullable[String].map(normaliseSpaces) and
        (__ \ "country").read[Country]
      )(InternationalAddress(_, _, _, _, _, _))
  }

  implicit val writes: OWrites[InternationalAddress] = (o: InternationalAddress) => {
    val line2Obj = o.line2.map(x => Json.obj("line2" -> x)).getOrElse(Json.obj())
    val stateOrRegionObj = o.stateOrRegion.map(x => Json.obj("stateOrRegion" -> x)).getOrElse(Json.obj())
    val postCodeObj = o.postCode.map(x => Json.obj("postCode" -> x)).getOrElse(Json.obj())

    Json.obj(
      "line1" -> o.line1,
      "townOrCity" -> o.townOrCity,
      "country" -> o.country
    ) ++ line2Obj ++ stateOrRegionObj ++ postCodeObj
  }

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
