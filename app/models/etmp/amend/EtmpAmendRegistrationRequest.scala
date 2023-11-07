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

import models.etmp._
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import models.UserAnswers
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDateTime

case class EtmpAmendRegistrationRequest(
                                    administration: EtmpAdministration,
                                    changeLog: EtmpAmendRegistrationChangeLog,
                                    customerIdentification: EtmpCustomerIdentification,
                                    tradingNames: Seq[EtmpTradingName],
                                    schemeDetails: EtmpSchemeDetails,
                                    bankDetails: EtmpBankDetails,
                                  )

object EtmpAmendRegistrationRequest {

  implicit val format: OFormat[EtmpAmendRegistrationRequest] = Json.format[EtmpAmendRegistrationRequest]

  def buildEtmpAmendRegistrationRequest(answers: UserAnswers, vrn: Vrn, commencementDate: LocalDateTime): EtmpAmendRegistrationRequest = {
    val etmpRegistrationRequest = buildEtmpRegistrationRequest(answers, vrn, commencementDate)
    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionAmend),
      changeLog = EtmpAmendRegistrationChangeLog( // TODO this needs calculating based on original registration
        tradingNames = false,
        fixedEstablishments = false,
        contactDetails = false,
        bankDetails = false
      ),
      customerIdentification = etmpRegistrationRequest.customerIdentification,
      tradingNames = etmpRegistrationRequest.tradingNames,
      schemeDetails = etmpRegistrationRequest.schemeDetails,
      bankDetails = etmpRegistrationRequest.bankDetails
    )
  }

}