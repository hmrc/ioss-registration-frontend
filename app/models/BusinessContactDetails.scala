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

import play.api.libs.json.*
import domain.ModelHelpers.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class BusinessContactDetails (
  fullName: String,
  telephoneNumber: String,
  emailAddress: String
)

object BusinessContactDetails {
  implicit val reads: Reads[BusinessContactDetails] = (

    (__ \ "fullName").read[String].map(normaliseSpaces) and
      (__ \ "telephoneNumber").read[String] and
      (__ \ "emailAddress").read[String]
    )(BusinessContactDetails.apply _)

  implicit val writes: Writes[BusinessContactDetails] = Json.writes[BusinessContactDetails]

  def apply(fullName: String, telephoneNumber: String, emailAddress: String): BusinessContactDetails =
    new BusinessContactDetails(normaliseSpaces(fullName), telephoneNumber, emailAddress)
}
