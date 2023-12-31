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

package controllers.actions

import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.BusinessContactDetails
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, Verified}
import models.requests.AuthenticatedDataRequest
import pages.BusinessContactDetailsPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmailVerificationFilterImpl(
                                        inAmend: Boolean,
                                        frontendAppConfig: FrontendAppConfig,
                                        emailVerificationService: EmailVerificationService,
                                        registrationConnector: RegistrationConnector
                                      )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (frontendAppConfig.emailVerificationEnabled && !inAmend) {
      request.userAnswers.get(BusinessContactDetailsPage) match {
        case Some(contactDetails) =>
          if(inAmend) {
            registrationConnector.getRegistration().flatMap {
              case Right(registrationWrapper) =>
                if(registrationWrapper.registration.schemeDetails.businessEmailId != contactDetails.emailAddress) {
                  checkVerificationStatusAndGetRedirect(request, contactDetails)
                } else {
                  None.toFuture
                }
              case Left(error) =>
                val exception = new Exception(s"Error when getting registration during email verification check on an amend joruney ${error.body}")
                logger.error(exception.getMessage, exception)
                throw exception
            }
          } else {
            checkVerificationStatusAndGetRedirect(request, contactDetails)
          }
        case None => None.toFuture
      }
    } else {
      None.toFuture
    }
  }

  private def checkVerificationStatusAndGetRedirect(
                                                     request: AuthenticatedDataRequest[_],
                                                     contactDetails: BusinessContactDetails
                                                   )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    emailVerificationService.isEmailVerified(contactDetails.emailAddress, request.userId).map {
      case Verified =>
        logger.info("CheckEmailVerificationFilter - Verified")
        None
      case LockedTooManyLockedEmails =>
        logger.info("CheckEmailVerificationFilter - LockedTooManyLockedEmails")
        Some(Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url))

      case LockedPasscodeForSingleEmail =>
        logger.info("CheckEmailVerificationFilter - LockedPasscodeForSingleEmail")
        Some(Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url))

      case _ =>
        logger.info("CheckEmailVerificationFilter - Not Verified")
        Some(Redirect(controllers.routes.BusinessContactDetailsController.onPageLoad().url))
    }
  }
}

class CheckEmailVerificationFilterProvider @Inject()(
                                                      frontendAppConfig: FrontendAppConfig,
                                                      emailVerificationService: EmailVerificationService,
                                                      registrationConnector: RegistrationConnector
                                                    )(implicit val executionContext: ExecutionContext) {
  def apply(inAmend: Boolean): CheckEmailVerificationFilterImpl = {
    new CheckEmailVerificationFilterImpl(inAmend, frontendAppConfig, emailVerificationService, registrationConnector)
  }
}

