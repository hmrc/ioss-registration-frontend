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

import controllers.routes
import logging.Logging
import models.requests.AuthenticatedDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CheckPartOfVatGroupFilterImpl(
                                     restrictFromPartOfVatGroup: Boolean
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {
    if (restrictFromPartOfVatGroup) {
      request.userAnswers.vatInfo.map { vatCustomerInfo =>
        if (vatCustomerInfo.partOfVatGroup) {
          Some(Redirect(routes.CannotAccessPageController.onPageLoad())).toFuture
        } else {
          None.toFuture
        }
      }.getOrElse {
        val errorMessage = "VAT info unavailable, must have VAT info."
        logger.error(errorMessage)
        val exception: IllegalStateException = new IllegalStateException(errorMessage)
        throw exception
      }
    } else {
      None.toFuture
    }
  }
}

class CheckPartOfVatGroupFilter @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(restrictFromPartOfVatGroup: Boolean): CheckPartOfVatGroupFilterImpl = {
    new CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup)
  }
}
