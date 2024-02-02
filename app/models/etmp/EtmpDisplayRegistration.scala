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

import date.LocalDateOps
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class EtmpDisplayRegistration(
                                    tradingNames: Seq[EtmpTradingName],
                                    schemeDetails: EtmpDisplaySchemeDetails,
                                    bankDetails: EtmpBankDetails,
                                    exclusions: Seq[EtmpExclusion],
                                    adminUse: EtmpAdminUse
                                  ) {


  def canRejoinRegistration(currentDate: LocalDate): Boolean = {
    exclusions.lastOption match {
      case Some(etmpExclusion) if etmpExclusion.exclusionReason == EtmpExclusionReason.Reversal => false
      case Some(etmpExclusion) if isQuarantinedAndAfterTwoYears(currentDate, etmpExclusion) => true
      case Some(etmpExclusion) if notQuarantinedAndAfterEffectiveDate(currentDate, etmpExclusion) => true
      case _ => false
    }
  }

  private def isQuarantinedAndAfterTwoYears(currentDate: LocalDate, etmpExclusion: EtmpExclusion): Boolean = {
    if(etmpExclusion.quarantine) {
      val minimumDate = currentDate.minusYears(2)
      etmpExclusion.effectiveDate.isBefore(minimumDate)
    } else {
      false
    }
  }

  private def notQuarantinedAndAfterEffectiveDate(currentDate: LocalDate, etmpExclusion: EtmpExclusion): Boolean = {
    if(!etmpExclusion.quarantine) {
      etmpExclusion.effectiveDate <= currentDate
    } else {
      false
    }
  }

}

object EtmpDisplayRegistration {

  implicit val format: OFormat[EtmpDisplayRegistration] = Json.format[EtmpDisplayRegistration]
}
