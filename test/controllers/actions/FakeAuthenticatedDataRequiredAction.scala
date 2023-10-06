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
import models.UserAnswers
import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import utils.FutureSyntax.FutureOps

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

case class FakeAuthenticatedDataRequiredAction(dataToReturn: Option[UserAnswers])
  extends AuthenticatedDataRequiredActionImpl(mock[RegistrationConnector])(ExecutionContext.Implicits.global) {

  private val emptyUserAnswers: UserAnswers = UserAnswers("12345-credId", lastUpdated = LocalDate.now.atStartOfDay(ZoneId.systemDefault()).toInstant)

  private val data: UserAnswers = dataToReturn match {
    case Some(data) => data
    case _ => emptyUserAnswers
  }

  override protected def refine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedDataRequest[A]]] = {
    Right(AuthenticatedDataRequest(request, request.credentials, request.vrn, None, data)).toFuture
  }
}
