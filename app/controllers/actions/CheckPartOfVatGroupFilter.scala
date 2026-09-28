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

package controllers.actions

import controllers.euDetails.routes as euRoutes
import logging.Logging
import models.CheckMode
import models.requests.AuthenticatedDataRequest
import pages.amend.ChangeRegistrationPage
import pages.{EmptyWaypoints, NonEmptyWaypoints, SavedProgressPage, Waypoint}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Call, Result}
import queries.euDetails.AllEuDetailsQuery
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckPartOfVatGroupFilterImpl(
                                     restrictFromPartOfVatGroup: Boolean,
                                     registrationModificationMode: RegistrationModificationMode
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    (restrictFromPartOfVatGroup, request.userAnswers.get(SavedProgressPage).nonEmpty, registrationModificationMode) match {
      case (true, false, AmendingActiveRegistration) =>
        val waypoints: NonEmptyWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
        determineFeAndRedirect(hasSavedAnswers = false, euRoutes.DeleteAllFixedEstablishmentsAsPartOfVatGroupController.onPageLoad(waypoints))(request)

      case (true, _, _) if request.userAnswers.vatInfo.exists(_.partOfVatGroup) =>
        determineFeAndRedirect(false, controllers.routes.CannotAccessPageController.onPageLoad())(request)

      case (_, true, NotModifyingExistingRegistration) =>
        val waypoints: EmptyWaypoints.type = EmptyWaypoints
        determineFeAndRedirect(hasSavedAnswers = true, controllers.routes.SavedProgressRemoveFixedEstablishmentsController.onPageLoad(waypoints))(request)

      case (_, _, _) => None.toFuture
    }
  }

  private def determineFeAndRedirect(hasSavedAnswers: Boolean, redirectCall: Call)(implicit request: AuthenticatedDataRequest[_]) = {
    (hasSavedAnswers, request.registrationWrapper) match {
      case (false, Some(registrationWrapper))
        if registrationWrapper.vatInfo.partOfVatGroup && registrationWrapper.registration.schemeDetails.euRegistrationDetails.nonEmpty =>
        Some(Redirect(redirectCall)).toFuture

      case (false, _) =>
        Some(Redirect(redirectCall)).toFuture

      case (true, _) =>
        request.userAnswers.vatInfo match {
          case Some(vatInfo) if vatInfo.partOfVatGroup && request.userAnswers.get(AllEuDetailsQuery).nonEmpty =>
            Some(Redirect(redirectCall)).toFuture

          case _ => None.toFuture
        }
    }
  }
}

class CheckPartOfVatGroupFilter @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(restrictFromPartOfVatGroup: Boolean, registrationModificationMode: RegistrationModificationMode): CheckPartOfVatGroupFilterImpl = {
    new CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup, registrationModificationMode)
  }
}
