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

package models.etmp

import play.api.libs.json.{Json, OFormat}

case class EtmpDisplaySchemeDetails(
                              commencementDate: String,
                              euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails],
                              previousEURegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails],
                              websites: Seq[EtmpWebsite],
                              contactName: String,
                              businessTelephoneNumber: String,
                              businessEmailId: String,
                              unusableStatus: Boolean,
                              nonCompliantReturns: Option[String],
                              nonCompliantPayments: Option[String]
                            )

object EtmpDisplaySchemeDetails {

  implicit val format: OFormat[EtmpDisplaySchemeDetails] = Json.format[EtmpDisplaySchemeDetails]
}