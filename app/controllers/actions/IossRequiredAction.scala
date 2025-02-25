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
import play.api.mvc.{ActionRefiner, Result}
import play.api.mvc.Results._
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IossRequiredActionImpl @Inject()()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest] with Logging {
  override protected def refine[A](request: AuthenticatedDataRequest[A]):
  Future[Either[Result, AuthenticatedMandatoryIossRequest[A]]] =
    request.iossNumber match {
      case None =>
        logger.info("insufficient IOSS enrolments")
        Left(Unauthorized).toFuture

      case Some(iossNumber) =>
        request.registrationWrapper match {
          case Some(registrationWrapper) =>
            Right(
              AuthenticatedMandatoryIossRequest(
                request,
                request.credentials,
                request.vrn,
                request.enrolments,
                iossNumber,
                registrationWrapper,
                request.userAnswers
              )
            ).toFuture
          case _ =>
            logger.error(s"Error: there was no registration present")
            Left(InternalServerError).toFuture
        }
    }
}

class IossRequiredAction @Inject()()(implicit executionContext: ExecutionContext) {

  def apply(): IossRequiredActionImpl = {
    new IossRequiredActionImpl()
  }
}
