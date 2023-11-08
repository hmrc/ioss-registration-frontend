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

import java.time.LocalDate

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

  def buildEtmpAmendRegistrationRequest(
                                         answers: UserAnswers,
                                         registration: EtmpDisplayRegistration,
                                         vrn: Vrn,
                                         commencementDate: LocalDate): EtmpAmendRegistrationRequest = {
    val etmpRegistrationRequest = buildEtmpRegistrationRequest(answers, vrn, commencementDate)

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionAmend),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames =
          registration.tradingNames != etmpRegistrationRequest.tradingNames,
        fixedEstablishments =
          registration.schemeDetails.euRegistrationDetails != etmpRegistrationRequest.schemeDetails.euRegistrationDetails,
        contactDetails =
          contactDetailsDiff(registration.schemeDetails, etmpRegistrationRequest.schemeDetails),
        bankDetails = registration.bankDetails != etmpRegistrationRequest.bankDetails
      ),
      customerIdentification = etmpRegistrationRequest.customerIdentification,
      tradingNames = etmpRegistrationRequest.tradingNames,
      schemeDetails = etmpRegistrationRequest.schemeDetails,
      bankDetails = etmpRegistrationRequest.bankDetails
    )
  }

  private def contactDetailsDiff(
                                  registrationSchemeDetails: EtmpSchemeDetails,
                                  amendSchemeDetails: EtmpSchemeDetails
                                ): Boolean = {
    registrationSchemeDetails.contactName != amendSchemeDetails.contactName ||
      registrationSchemeDetails.businessTelephoneNumber != amendSchemeDetails.businessTelephoneNumber ||
      registrationSchemeDetails.businessEmailId != amendSchemeDetails.businessEmailId
  }

}