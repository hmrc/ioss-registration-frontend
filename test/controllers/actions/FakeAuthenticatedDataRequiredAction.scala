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

import connectors.RegistrationConnector
import models.amend.RegistrationWrapper
import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest}
import models.{CompositeAccount, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import utils.FutureSyntax.FutureOps

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthenticatedDataRequiredAction(
                                           dataToReturn: Option[UserAnswers],
                                           registrationWrapper: Option[RegistrationWrapper],
                                           compositeAccount: Option[CompositeAccount],
                                           numberOfIossRegistrations: Int
                                         )
  extends AuthenticatedDataRequiredActionImpl(mock[RegistrationConnector], isInAmendMode = false)(ExecutionContext.Implicits.global) {

  private val emptyUserAnswers: UserAnswers = UserAnswers("12345-credId", lastUpdated = LocalDate.now.atStartOfDay(ZoneId.systemDefault()).toInstant)

  private val data: UserAnswers = dataToReturn match {
    case Some(data) => data
    case _ => emptyUserAnswers
  }

  override protected def refine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedDataRequest[A]]] = {
    Right(AuthenticatedDataRequest(
      request = request,
      credentials = request.credentials,
      vrn = request.vrn,
      enrolments = request.enrolments,
      iossNumber = request.iossNumber,
      userAnswers = data,
      registrationWrapper = registrationWrapper,
      numberOfIossRegistrations = numberOfIossRegistrations,
      compositeAccount = compositeAccount
    )).toFuture
  }
}

class FakeAuthenticatedDataRequiredActionProvider(
                                                   dataToReturn: Option[UserAnswers],
                                                   registrationWrapper: Option[RegistrationWrapper],
                                                   compositeAccount: Option[CompositeAccount],
                                                   numberOfIossRegistrations: Int
                                                 )
  extends AuthenticatedDataRequiredAction(mock[RegistrationConnector])(ExecutionContext.Implicits.global) {

  override def apply(isInAmendMode: Boolean): AuthenticatedDataRequiredActionImpl = {
    new FakeAuthenticatedDataRequiredAction(dataToReturn, registrationWrapper, compositeAccount, numberOfIossRegistrations)
  }
}


