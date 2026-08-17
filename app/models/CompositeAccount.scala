/*
 * Copyright 2026 HM Revenue & Customs
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

import models.etmp.{EtmpBankDetails, EtmpTradingName}
import play.api.libs.json.{Json, OFormat}

case class CompositeAccount(
                             tradingNames: Seq[TradingName],
                             contactDetails: BusinessContactDetails,
                             bankDetails: BankDetails
                           )

object CompositeAccount {

  def fromEtmpTradingName(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] = {
    etmpTradingNames.map(tn => TradingName(tn.tradingName))
  }

  def fromEtmpBankDetails(etmpBankDetails: EtmpBankDetails): BankDetails = {
    BankDetails(
      accountName = etmpBankDetails.accountName,
      bic = etmpBankDetails.bic,
      iban = etmpBankDetails.iban
    )
  }
  
  implicit val format: OFormat[CompositeAccount] = Json.format[CompositeAccount]
}
