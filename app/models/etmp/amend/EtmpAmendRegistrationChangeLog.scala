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

package models.etmp.amend

import play.api.libs.json.{Json, OFormat, Reads, Writes}

trait EtmpAmendRegistrationChangeLog

object EtmpAmendRegistrationChangeLog {

  implicit val reads: Reads[EtmpAmendRegistrationChangeLog] =
    EtmpAmendRegistrationChangeLogNew.format.widen[EtmpAmendRegistrationChangeLog] orElse
      EtmpAmendRegistrationChangeLogLegacy.format.widen[EtmpAmendRegistrationChangeLog]

  implicit val writes: Writes[EtmpAmendRegistrationChangeLog] = Writes {
    case earcln: EtmpAmendRegistrationChangeLogNew => Json.toJson(earcln)(EtmpAmendRegistrationChangeLogNew.format)
    case earcll: EtmpAmendRegistrationChangeLogLegacy => Json.toJson(earcll)(EtmpAmendRegistrationChangeLogLegacy.format)
  }
}

case class EtmpAmendRegistrationChangeLogLegacy(
                                                 tradingNames: Boolean,
                                                 fixedEstablishments: Boolean,
                                                 contactDetails: Boolean,
                                                 bankDetails: Boolean,
                                                 reRegistration: Boolean
                                               ) extends EtmpAmendRegistrationChangeLog

object EtmpAmendRegistrationChangeLogLegacy {

  implicit val format: OFormat[EtmpAmendRegistrationChangeLogLegacy] = Json.format[EtmpAmendRegistrationChangeLogLegacy]

}

case class EtmpAmendRegistrationChangeLogNew(
                                              tradingNames: Boolean,
                                              fixedEstablishments: Boolean,
                                              contactDetails: Boolean,
                                              bankDetails: Boolean,
                                              reRegistration: Boolean,
                                              otherAddress: Boolean
                                            ) extends EtmpAmendRegistrationChangeLog

object EtmpAmendRegistrationChangeLogNew {

  implicit val format: OFormat[EtmpAmendRegistrationChangeLogNew] = Json.format[EtmpAmendRegistrationChangeLogNew]

}