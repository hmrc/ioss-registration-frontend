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

import connectors.RegistrationConnector
import controllers.filters.{routes => filterRoutes}
import controllers.routes
import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest, UnauthenticatedDataRequest, UnauthenticatedOptionalDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedDataRequiredActionImpl @Inject()(
                                                     val registrationConnector: RegistrationConnector
                                                   )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthenticatedOptionalDataRequest, AuthenticatedDataRequest] {

  override protected def refine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedDataRequest[A]]] = {

    request.userAnswers match {
      case None =>
        Left(Redirect(routes.JourneyRecoveryController.onPageLoad())).toFuture
      case Some(data) =>
        Right(AuthenticatedDataRequest(request.request, request.credentials, request.vrn, request.iossNumber, data)).toFuture
    }
  }
}

class UnauthenticatedDataRequiredAction @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[UnauthenticatedOptionalDataRequest, UnauthenticatedDataRequest] {

  override protected def refine[A](request: UnauthenticatedOptionalDataRequest[A]): Future[Either[Result, UnauthenticatedDataRequest[A]]] = {

    request.userAnswers match {
      case None =>
        Left(Redirect(filterRoutes.RegisteredForIossInEuController.onPageLoad())).toFuture
      case Some(data) =>
        Right(UnauthenticatedDataRequest(request.request, request.userId, data)).toFuture
    }
  }
}

class AuthenticatedDataRequiredAction @Inject()(
                                                 registrationConnector: RegistrationConnector
                                               )(implicit executionContext: ExecutionContext) {

  def apply(): AuthenticatedDataRequiredActionImpl = {
    new AuthenticatedDataRequiredActionImpl(registrationConnector)
  }
}
