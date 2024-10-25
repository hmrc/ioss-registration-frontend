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
import controllers.routes
import logging.Logging
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, Verified}
import models.requests.AuthenticatedDataRequest
import models.{BusinessContactDetails, CheckMode, NormalMode}
import pages.amend.ChangeRegistrationPage
import pages.{BankDetailsPage, BusinessContactDetailsPage, CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.{EmailVerificationService, SaveForLaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmailVerificationFilterImpl(
                                        inAmend: Boolean,
                                        waypoints: Waypoints,
                                        frontendAppConfig: FrontendAppConfig,
                                        emailVerificationService: EmailVerificationService,
                                        saveForLaterService: SaveForLaterService,
                                        registrationConnector: RegistrationConnector
                                      )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (frontendAppConfig.emailVerificationEnabled) {
      request.userAnswers.get(BusinessContactDetailsPage) match {
        case Some(contactDetails) =>
          if (inAmend) {
            registrationConnector.getRegistration()(hc).flatMap {
              case Right(registrationWrapper) =>
                if (registrationWrapper.registration.schemeDetails.businessEmailId != contactDetails.emailAddress) {
                  checkVerificationStatusAndGetRedirect(waypoints, request, contactDetails)
                } else {
                  None.toFuture
                }
              case Left(error) =>
                val exception = new Exception(s"Error when getting registration during email verification check on an amend joruney ${error.body}")
                logger.error(exception.getMessage, exception)
                throw exception
            }
          } else {
            checkVerificationStatusAndGetRedirect(waypoints, request, contactDetails)
          }
        case None => None.toFuture
      }
    } else {
      None.toFuture
    }
  }

  private def checkVerificationStatusAndGetRedirect(
                                                     waypoints: Waypoints,
                                                     request: AuthenticatedDataRequest[_],
                                                     contactDetails: BusinessContactDetails
                                                   )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    emailVerificationService.isEmailVerified(contactDetails.emailAddress, request.userId).flatMap {
      case Verified =>
        logger.info("CheckEmailVerificationFilter - Verified")
        None.toFuture
      case LockedTooManyLockedEmails =>
        logger.info("CheckEmailVerificationFilter - LockedTooManyLockedEmails")
        Some(Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)).toFuture

      case LockedPasscodeForSingleEmail =>
        logger.info("CheckEmailVerificationFilter - LockedPasscodeForSingleEmail")
        saveForLaterService.saveAnswersRedirect(
          waypoints,
          routes.EmailVerificationCodesExceededController.onPageLoad().url,
          request.uri
        )(request, executionContext, hc).map(result => Some(result))

      case _ =>
        logger.info("CheckEmailVerificationFilter - Not Verified")
        val bankDetails = request.userAnswers.get(BankDetailsPage)
        val waypoints = {
          if (bankDetails.isEmpty) {
            EmptyWaypoints.setNextWaypoint(
              Waypoint(CheckYourAnswersPage, NormalMode, CheckYourAnswersPage.urlFragment)
            )
          } else {
            EmptyWaypoints.setNextWaypoint(
              Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment)
            )
          }
        }
        Some(Redirect(routes.BusinessContactDetailsController.onPageLoad(waypoints).url)).toFuture
    }
  }
}

class CheckEmailVerificationFilterProvider @Inject()(
                                                      frontendAppConfig: FrontendAppConfig,
                                                      emailVerificationService: EmailVerificationService,
                                                      saveForLaterService: SaveForLaterService,
                                                      registrationConnector: RegistrationConnector
                                                    )(implicit val executionContext: ExecutionContext) {
  def apply(inAmend: Boolean, waypoints: Waypoints): CheckEmailVerificationFilterImpl = {
    new CheckEmailVerificationFilterImpl(inAmend, waypoints, frontendAppConfig, emailVerificationService, saveForLaterService, registrationConnector)
  }
}

