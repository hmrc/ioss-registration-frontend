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
import models.requests.AuthenticatedDataRequest
import pages.{EmptyWaypoints, SavedProgressPage}
import play.api.libs.json.Json
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
      case (_, _, AmendingActiveRegistration) =>
        println("a")
        determineFeAndRedirect(hasSavedAnswers = false, euRoutes.DeleteAllFixedEstablishmentsAsPartOfVatGroupController.onPageLoad())(request)

      case (true, _, _) if isVatGroup()(request) =>
        println("b")
        Some(Redirect(controllers.routes.CannotAccessPageController.onPageLoad())).toFuture
      case (_, true, NotModifyingExistingRegistration) =>
        println("c")
        val waypoints: EmptyWaypoints.type = EmptyWaypoints
        determineFeAndRedirect(hasSavedAnswers = true, controllers.routes.SavedProgressRemoveFixedEstablishmentsController.onPageLoad(waypoints))(request)

      case (_, _, _) =>
        println("d")
        None.toFuture
    }
  }

  private def isVatGroup()(implicit request: AuthenticatedDataRequest[_]): Boolean = {
    request.registrationWrapper match {
      case Some(registrationWrapper) if registrationWrapper.vatInfo.partOfVatGroup =>
        true
      case _ =>
        request.userAnswers.vatInfo match {
          case Some(vatInfo) if vatInfo.partOfVatGroup => true
          case _ => false
        }
    }
  }

  private def determineFeAndRedirect(hasSavedAnswers: Boolean, redirectCall: Call)(implicit request: AuthenticatedDataRequest[_]) = {
    (hasSavedAnswers, request.registrationWrapper) match {
      case (false, Some(registrationWrapper))
        if registrationWrapper.vatInfo.partOfVatGroup && registrationWrapper.registration.schemeDetails.euRegistrationDetails.nonEmpty =>
        println("1")
        Some(Redirect(redirectCall)).toFuture

      case (true, _) =>
        request.userAnswers.vatInfo match {
          case Some(vatInfo) if vatInfo.partOfVatGroup && request.userAnswers.get(AllEuDetailsQuery).nonEmpty =>
            println("2")
            Some(Redirect(redirectCall)).toFuture

          case _ =>
            println("3")
            None.toFuture
        }

      case (_, _) =>
        println(s"4 $hasSavedAnswers ${Json.toJson(request.registrationWrapper.map(_.vatInfo.partOfVatGroup))}")
        None.toFuture
    }
  }
}

class CheckPartOfVatGroupFilter @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(restrictFromPartOfVatGroup: Boolean, registrationModificationMode: RegistrationModificationMode): CheckPartOfVatGroupFilterImpl = {
    new CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup, registrationModificationMode)
  }
}
