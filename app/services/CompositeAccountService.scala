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

package services

import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.CompositeAccount.fromEtmpBankDetails
import models.intermediaries.EtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.{BankDetails, BusinessContactDetails, CompositeAccount, TradingName}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompositeAccountService @Inject()(
                                         registrationConnector: RegistrationConnector,
                                         frontendAppConfig: FrontendAppConfig
                                       )(implicit ec: ExecutionContext) extends Logging {

  def getCompositeAccount(enrolments: Enrolments, vrn: Vrn)(implicit hc: HeaderCarrier): Future[Option[CompositeAccount]] = {

    val hasOssEnrolment = enrolments.enrolments.exists(_.key == frontendAppConfig.ossEnrolment)
    val hasIntermediaryEnrolment = enrolments.enrolments.exists(_.key == frontendAppConfig.intermediaryEnrolment)

    if (hasOssEnrolment) {
      getLatestOssRegistration(vrn).map {
        case Some(ossRegistration) =>
        createCompositeAccount(
          tradingNames = ossRegistration.tradingNames,
          fullName = ossRegistration.contactDetails.fullName,
          telephoneNumber = ossRegistration.contactDetails.telephoneNumber,
          emailAddress = ossRegistration.contactDetails.emailAddress,
          bankDetails = ossRegistration.bankDetails
        )

        case _ => None
      }
    } else if (hasIntermediaryEnrolment) {
      getIntermediaryRegistration(enrolments).map {
        case Some(intermediaryRegistration) =>
          createCompositeAccount(
            tradingNames = intermediaryRegistration.tradingNames.map(_.tradingName),
            fullName = intermediaryRegistration.schemeDetails.contactName,
            telephoneNumber = intermediaryRegistration.schemeDetails.businessTelephoneNumber,
            emailAddress = intermediaryRegistration.schemeDetails.businessEmailId,
            bankDetails = fromEtmpBankDetails(intermediaryRegistration.bankDetails)
          )

        case _ => None
      }
    } else {
      None.toFuture
    }
  }

  private def getIntermediaryRegistration(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[EtmpDisplayRegistration]] = {
    getIntermediaryEnrolment(enrolments, frontendAppConfig.intermediaryEnrolment, "IntNumber") match {
      case Some(intermediaryNumber) =>
        registrationConnector.getIntermediaryRegistration(intermediaryNumber).flatMap {
          case Right(etmpDisplayRegistration) =>
            Some(etmpDisplayRegistration).toFuture

          case Left(_) =>
            None.toFuture
        }

      case _ =>
        None.toFuture
    }
  }

  private def createCompositeAccount(
                                      tradingNames: Seq[String],
                                      fullName: String,
                                      telephoneNumber: String,
                                      emailAddress: String,
                                      bankDetails: BankDetails
                                    ): Option[CompositeAccount] = {
    Some(CompositeAccount(
      tradingNames = tradingNames.map(TradingName(_)),
      contactDetails = BusinessContactDetails(
        fullName = fullName,
        telephoneNumber = telephoneNumber,
        emailAddress = emailAddress
      ),
      bankDetails = bankDetails
    ))
  }
  
  private def getLatestOssRegistration(vrn: Vrn)(implicit hc: HeaderCarrier): Future[Option[OssRegistration]] = {
    registrationConnector.getOssRegistration(vrn).map {
      case Right(ossRegistration) => Some(ossRegistration)
      case Left(_) => None
    }
  }

  private def getIntermediaryEnrolment(
                                        enrolments: Enrolments,
                                        key: String,
                                        identifierKey: String
                                      ): Option[String] = {
    enrolments.getEnrolment(key)
      .flatMap(_.identifiers
        .find(_.key == identifierKey)
        .map(_.value)
      )
  }
}