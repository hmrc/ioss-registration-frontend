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

package controllers

import controllers.actions._
import logging.Logging
import models.requests.AuthenticatedDataRequest
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.rejoin.RejoinRegistrationPage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.Gettable
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait AnswerExtractor extends Logging {

  def getAnswer[A](waypoints: Waypoints, query: Gettable[A])
                  (block: A => Result)
                  (implicit request: AuthenticatedDataRequest[AnyContent], ev: Reads[A]): Result =
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse({
        if (waypoints.inAmend) {
          waypoints.registrationModificationMode match {
            case RejoiningRegistration => Redirect(RejoinRegistrationPage.route(waypoints))
            case AmendingActiveRegistration => Redirect(ChangeRegistrationPage.route(waypoints))
            case AmendingPreviousRegistration => Redirect(ChangePreviousRegistrationPage.route(waypoints))
            case NotModifyingExistingRegistration =>
              logAnswerNotFoundMessage(query)
              Redirect(JourneyRecoveryPage.route(waypoints))
          }
      } else {
        logAnswerNotFoundMessage(query)
        Redirect(JourneyRecoveryPage.route(waypoints))
      }})

  def getAnswerAsync[A](waypoints: Waypoints, query: Gettable[A])
                       (block: A => Future[Result])
                       (implicit request: AuthenticatedDataRequest[AnyContent], ev: Reads[A]): Future[Result] =
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse({
        if (waypoints.inAmend) {
          waypoints.registrationModificationMode match {
            case RejoiningRegistration => Redirect(RejoinRegistrationPage.route(waypoints)).toFuture
            case AmendingActiveRegistration => Redirect(ChangeRegistrationPage.route(waypoints)).toFuture
            case AmendingPreviousRegistration => Redirect(ChangePreviousRegistrationPage.route(waypoints)).toFuture
            case NotModifyingExistingRegistration =>
              logAnswerNotFoundMessage(query)
              Redirect(JourneyRecoveryPage.route(waypoints)).toFuture
          }
        } else {
          logAnswerNotFoundMessage(query)
          Redirect(JourneyRecoveryPage.route(waypoints)).toFuture
        }
      })

  private def logAnswerNotFoundMessage[T](query: Gettable[T]): Unit = logger.warn(s"$query question has not been answered")
}
