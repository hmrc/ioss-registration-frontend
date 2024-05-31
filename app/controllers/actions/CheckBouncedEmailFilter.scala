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
import controllers.routes
import logging.Logging
import models.CheckMode
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.requests.AuthenticatedMandatoryIossRequest
import pages.amend.ChangeRegistrationPage
import pages.{BusinessContactDetailsPage, EmptyWaypoints, Waypoint}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckBouncedEmailFilterImpl(
                                   frontendAppConfig: FrontendAppConfig,
                                   emailVerificationService: EmailVerificationService
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedMandatoryIossRequest] with Logging {

  override protected def filter[A](request: AuthenticatedMandatoryIossRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val emailAddress = request.registrationWrapper.registration.schemeDetails.businessEmailId
    val emailMatched = request.userAnswers.get(BusinessContactDetailsPage).exists(_.emailAddress == emailAddress)

    if (request.registrationWrapper.registration.schemeDetails.unusableStatus && emailMatched && frontendAppConfig.emailVerificationEnabled) {
      checkVerificationStatusAndGetRedirect(request.request.userId, emailAddress)
    } else {
      Future(None)
    }
  }

  private def checkVerificationStatusAndGetRedirect(
                                                     userId: String,
                                                     emailAddress: String
                                                   )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    emailVerificationService.isEmailVerified(emailAddress, userId).flatMap {
      case Verified =>
        logger.info("CheckBouncedEmailFilter - Verified")
        None.toFuture

      case LockedTooManyLockedEmails =>
        logger.info("CheckEmailVerificationFilter - LockedTooManyLockedEmails")
        Some(Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)).toFuture

      case LockedPasscodeForSingleEmail =>
        logger.info("CheckBouncedEmailFilter - LockedPasscodeForSingleEmail")
        Some(Redirect(routes.EmailVerificationCodesExceededController.onPageLoad().url)).toFuture

      case NotVerified =>
        logger.info("CheckBouncedEmailFilter - Not Verified")
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
        Some(Redirect(routes.BusinessContactDetailsController.onPageLoad(waypoints).url)).toFuture
    }
  }
}

class CheckBouncedEmailFilterProvider @Inject()(
                                                 frontendAppConfig: FrontendAppConfig,
                                                 emailVerificationService: EmailVerificationService
                                               )(implicit ec: ExecutionContext) {

  def apply(): CheckBouncedEmailFilterImpl =
    new CheckBouncedEmailFilterImpl(frontendAppConfig, emailVerificationService)

}