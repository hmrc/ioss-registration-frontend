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

package controllers.rejoin

import connectors.{RegistrationConnector, ReturnStatusConnector}
import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import controllers.actions.{AuthenticatedControllerComponents, RejoiningRegistration}
import controllers.rejoin.validation.RejoinRegistrationValidation
import logging.Logging
import models.requests.AuthenticatedMandatoryIossRequest
import models.responses.ErrorResponse
import models.{CheckMode, CurrentReturns, SubmissionStatus}
import pages.rejoin.{CannotRejoinRegistrationPage, RejoinRegistrationPage}
import pages.{EmptyWaypoints, NonEmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartRejoinJourneyController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationConnector: RegistrationConnector,
                                              returnStatusConnector: ReturnStatusConnector,
                                              rejoinRegistrationValidator: RejoinRegistrationValidation,
                                              registrationService: RegistrationService,
                                              authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository,
                                              clock: Clock
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with Logging {
  protected def controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIoss(RejoiningRegistration).async {
    implicit request: AuthenticatedMandatoryIossRequest[AnyContent] =>
      (for {
        registrationWrapperResponse <- registrationConnector.getRegistration()
        currentReturnsResponse <- returnStatusConnector.getCurrentReturns(request.iossNumber)
      } yield {
        val currentReturns = getResponseValue(currentReturnsResponse)
        val registrationWrapper = getResponseValue(registrationWrapperResponse)

        if (registrationWrapper.registration.canRejoinRegistration(LocalDate.now(clock)) && !existsOutstandingReturns(currentReturns)) {
          val thisPage = RejoinRegistrationPage
          val waypoints: NonEmptyWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, RejoinRegistrationPage.urlFragment))

          rejoinRegistrationValidator.validateEuRegistrations(registrationWrapper, waypoints)(hc(request), request.request, ec).flatMap {
            case Left(redirect) =>
              logger.info(s"Failed validating eu registrations, redirecting to '${redirect.url}'")
              Future.successful(Redirect(redirect))

            case _ =>
              for {
                userAnswers <- registrationService.toUserAnswers(request.userId, registrationWrapper)
                _ <- authenticatedUserAnswersRepository.set(userAnswers)
              } yield Redirect(RejoinRegistrationPage.route(waypoints).url)
          }
        } else {
          logger.warn("Cannot rejoin registration")
          Future.successful(Redirect(CannotRejoinRegistrationPage.route(waypoints).url))
        }
      }).flatten
  }

  private def getResponseValue[A](response: Either[ErrorResponse, A]): A = {
    response match {
      case Right(value) => value
      case Left(error) =>
        val exception = new Exception(error.body)
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def existsOutstandingReturns(currentReturns: CurrentReturns): Boolean = {
    val existsOutstandingReturn = {
      if (currentReturns.finalReturnsCompleted) {
        false
      } else {
        currentReturns.returns.exists { currentReturn =>
          Seq(SubmissionStatus.Due, SubmissionStatus.Overdue, SubmissionStatus.Next).contains(currentReturn.submissionStatus) &&
            !isOlderThanThreeYears(currentReturn.dueDate, clock)
        }
      }
    }
    existsOutstandingReturn
  }
}
