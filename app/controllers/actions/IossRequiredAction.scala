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

import logging.Logging
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IossRequiredAction @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest] with Logging {
  override protected def refine[A](request: AuthenticatedDataRequest[A]):
  Future[Either[Result, AuthenticatedMandatoryIossRequest[A]]] =
    request.iossNumber match {
      case None =>
        logger.info("insufficient IOSS enrolments")
        Left(Unauthorized).toFuture

      case Some(iossNumber) =>
        Right(
          AuthenticatedMandatoryIossRequest(
            request, request.credentials, request.vrn, iossNumber, request.userAnswers
          )
        ).toFuture
    }
}
