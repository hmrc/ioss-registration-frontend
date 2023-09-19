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

import models.Index
import models.requests.AuthenticatedDataRequest
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MaximumIndexFilter(index: Index, maxAllowed: Int)
                        (implicit val executionContext: ExecutionContext) extends ActionFilter[AuthenticatedDataRequest] {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] =
    if (index.position >= maxAllowed) {
      Some(NotFound).toFuture
    } else {
      None.toFuture
    }
}


class MaximumIndexFilterProvider @Inject()(implicit executionContext: ExecutionContext) {

  def apply(index: Index, maxAllowed: Int): MaximumIndexFilter = new MaximumIndexFilter(index, maxAllowed)
}