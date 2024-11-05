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

package services

import config.{Constants, FrontendAppConfig}
import connectors.EmailVerificationConnector
import connectors.EmailVerificationHttpParser.{ReturnEmailVerificationResponse, ReturnVerificationStatus}
import controllers.routes
import logging.Logging
import models.emailVerification.PasscodeAttemptsStatus.NotVerified
import models.emailVerification.{EmailVerificationRequest, PasscodeAttemptsStatus, VerifyEmail}
import pages.Waypoints
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject()(
                                          config: FrontendAppConfig,
                                          validateEmailConnector: EmailVerificationConnector
                                        )(implicit ec: ExecutionContext) extends Logging {


  def createEmailVerificationRequest(waypoints : Waypoints,
                                      credId: String,
                                      emailAddress: String,
                                      pageTitle: Option[String],
                                      continueUrl: String
                                    )(implicit hc: HeaderCarrier): Future[ReturnEmailVerificationResponse] = {
    validateEmailConnector.verifyEmail(
      EmailVerificationRequest(
        credId = credId,
        continueUrl = continueUrl,
        origin = config.origin,
        deskproServiceName = Some("ioss-registration-frontend"),
        accessibilityStatementUrl = config.accessibilityStatementUrl,
        pageTitle = pageTitle,
        backUrl = Some(routes.BusinessContactDetailsController.onPageLoad(waypoints).url),
        email = Some(
          VerifyEmail(
            address = emailAddress,
            enterUrl = routes.BusinessContactDetailsController.onPageLoad(waypoints).url
          )
        )
      )
    )
  }

  def getStatus(credId: String)(implicit hc: HeaderCarrier): Future[ReturnVerificationStatus] = {
    validateEmailConnector.getStatus(credId)
  }

  def isEmailVerified(emailAddress: String, credId: String)(implicit hc: HeaderCarrier): Future[PasscodeAttemptsStatus] = {
    getStatus(credId).map {
      case Right(Some(verificationStatus)) =>
        (verificationStatus.emails.count(_.locked) >= Constants.emailVerificationMaxEmails,
          verificationStatus.emails.exists(_.locked),
          verificationStatus.emails.exists(_.verified)) match {
          case (true, true, false) =>
            logger.info("Locked - too many email address verifications attempted")
            PasscodeAttemptsStatus.LockedTooManyLockedEmails

          case (false, true, false) if verificationStatus.emails.exists(_.emailAddress.equalsIgnoreCase(emailAddress)) =>
            logger.info("Locked - Too many verification attempts on this email address")
            PasscodeAttemptsStatus.LockedPasscodeForSingleEmail

          case (false, _, true) if verificationStatus.emails.exists(_.emailAddress.equalsIgnoreCase(emailAddress)) =>
            logger.info("Email address verified")
            PasscodeAttemptsStatus.Verified

          case _ =>
            logger.info("Email address not verified")
            NotVerified
        }

      case _ =>
        logger.warn("Received an unexpected verification status")
        NotVerified
    }
  }

}

