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
import logging.Logging
import models.requests.AuthenticatedIdentifierRequest
import pages.EmptyWaypoints
import pages.filters.CannotRegisterAlreadyRegisteredPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckRegistrationFilterImpl(
                                   inAmend: Boolean,
                                   frontendAppConfig: FrontendAppConfig
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedIdentifierRequest] with Logging {
  override protected def filter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = {
    (hasIossEnrolment(request), inAmend) match {
      case (true, false) =>
        Some(Redirect(CannotRegisterAlreadyRegisteredPage.route(EmptyWaypoints).url)).toFuture
      case _ =>
        None.toFuture
    }
  }

  private def hasIossEnrolment(request: AuthenticatedIdentifierRequest[_]): Boolean = {
    request.enrolments.enrolments.exists(_.key == frontendAppConfig.iossEnrolment)
  }
}

class CheckRegistrationFilterProvider @Inject()(
                                                 frontendAppConfig: FrontendAppConfig
                                               )(implicit executionContext: ExecutionContext) {

  def apply(inAmend: Boolean): CheckRegistrationFilterImpl = {
    new CheckRegistrationFilterImpl(inAmend, frontendAppConfig)
  }
}
