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

import models.{Enumerable, WithName}
import play.api.libs.json.*

sealed trait OssEuTaxIdentifierType

object OssEuTaxIdentifierType extends Enumerable.Implicits {

  case object Vat extends WithName("vat") with OssEuTaxIdentifierType
  case object Other extends WithName("other") with OssEuTaxIdentifierType

  val values: Seq[OssEuTaxIdentifierType] =
    Seq(Vat, Other)

  implicit val enumerable: Enumerable[OssEuTaxIdentifierType] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit def reads: Reads[OssEuTaxIdentifierType] = Reads[OssEuTaxIdentifierType] {
    case JsString(Vat.toString)   => JsSuccess(Vat)
    case JsString(Other.toString) => JsSuccess(Other)
    case _                        => JsError("error.invalid")
  }

  implicit def writes: Writes[OssEuTaxIdentifierType] = {
    Writes(value => JsString(value.toString))
  }
}
