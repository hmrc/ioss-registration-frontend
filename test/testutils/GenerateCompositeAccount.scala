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

package testutils

import models.intermediaries.EtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.{BankDetails, BusinessContactDetails, CompositeAccount, TradingName}

object GenerateCompositeAccount {

  def generateCompositeAccount(
                                ossRegistration: Option[OssRegistration] = None,
                                intermediaryRegistration: Option[EtmpDisplayRegistration] = None
                              ): Option[CompositeAccount] = {
    (ossRegistration, intermediaryRegistration) match {
      case (Some(ossReg), _) =>
        Some(createCompositeAccount(
          tradingNames = ossReg.tradingNames.map(TradingName(_)),
          fullName = ossReg.contactDetails.fullName,
          telephoneNumber = ossReg.contactDetails.telephoneNumber,
          emailAddress = ossReg.contactDetails.emailAddress,
          bankDetails = ossReg.bankDetails
        ))

      case (_, Some(intReg)) =>
        Some(createCompositeAccount(
          tradingNames = intReg.tradingNames.map(tn => TradingName(tn.tradingName)),
          fullName = intReg.schemeDetails.contactName,
          telephoneNumber = intReg.schemeDetails.businessTelephoneNumber,
          emailAddress = intReg.schemeDetails.businessEmailId,
          bankDetails =
            BankDetails(
              accountName = intReg.bankDetails.accountName,
              bic = intReg.bankDetails.bic,
              iban = intReg.bankDetails.iban
            )
        ))

      case _ => None
    }
  }

  private def createCompositeAccount(
                                      tradingNames: Seq[TradingName],
                                      fullName: String,
                                      telephoneNumber: String,
                                      emailAddress: String,
                                      bankDetails: BankDetails
                                    ): CompositeAccount = {
    CompositeAccount(
      tradingNames = tradingNames,
      contactDetails = BusinessContactDetails(
        fullName = fullName,
        telephoneNumber = telephoneNumber,
        emailAddress = emailAddress
      ),
      bankDetails = bankDetails
    )
  }
}
