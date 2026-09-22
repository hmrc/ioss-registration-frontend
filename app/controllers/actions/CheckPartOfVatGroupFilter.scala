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
import controllers.routes
import logging.Logging
import models.CheckMode
import models.requests.AuthenticatedDataRequest
import pages.amend.ChangeRegistrationPage
import pages.rejoin.RejoinRegistrationPage
import pages.{EmptyWaypoints, NonEmptyWaypoints, Waypoint}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckPartOfVatGroupFilterImpl(
                                     restrictFromPartOfVatGroup: Boolean,
                                     modifyingExistingRegistration: Boolean,
                                     registrationModificationMode: RegistrationModificationMode
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {
    if (restrictFromPartOfVatGroup) {
      request.registrationWrapper.map { registrationWrapper =>
        if (registrationWrapper.vatInfo.partOfVatGroup) {
          // TODO -> Test
          if (modifyingExistingRegistration) {
              if (registrationWrapper.registration.schemeDetails.euRegistrationDetails.nonEmpty) {
                // TODO -> Redirect to new page and delete FE

                val waypoints = determineWaypoints(registrationModificationMode)
                Some(Redirect(euRoutes.DeleteAllFixedEstablishmentsAsPartOfVatGroupController.onPageLoad(waypoints))).toFuture
              } else {
                None.toFuture
              }
          } else {
            Some(Redirect(routes.CannotAccessPageController.onPageLoad())).toFuture
          }
        } else {
          None.toFuture
        }
      }.getOrElse {
        throwException("Registration unavailable, must have a Registration.")
      }
    } else {
      None.toFuture
    }
  }

  private def throwException(errorMessage: String): Future[Option[Result]] = {
    logger.error(errorMessage)
    val exception: IllegalStateException = new IllegalStateException(errorMessage)
    throw exception
  }
  
  private def determineWaypoints(registrationModificationMode: RegistrationModificationMode): NonEmptyWaypoints = {
    registrationModificationMode match {
      case AmendingActiveRegistration =>
        EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
        
      case RejoiningRegistration =>
        EmptyWaypoints.setNextWaypoint(Waypoint(RejoinRegistrationPage, CheckMode, RejoinRegistrationPage.urlFragment))
        
        // TODO -> case _
    }
  }
}

class CheckPartOfVatGroupFilter @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(restrictFromPartOfVatGroup: Boolean, modifyingExistingRegistration: Boolean, registrationModificationMode: RegistrationModificationMode): CheckPartOfVatGroupFilterImpl = {
    new CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup, modifyingExistingRegistration, registrationModificationMode)
  }
}
