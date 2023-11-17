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

import connectors.SaveForLaterConnector
import controllers.actions.FakeSavedAnswersRetrievalAction.{mockSaveForLaterConnector, mockSessionRepository}
import models.UserAnswers
import models.requests.AuthenticatedOptionalDataRequest
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.domain.Vrn
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.{ExecutionContext, Future}

class FakeSavedAnswersRetrievalAction(dataToReturn: Option[UserAnswers], vrn: Vrn)
  extends SavedAnswersRetrievalAction(mockSessionRepository, mockSaveForLaterConnector)(ExecutionContext.Implicits.global) {

  override protected def transform[A](request: AuthenticatedOptionalDataRequest[A]): Future[AuthenticatedOptionalDataRequest[A]] =
    Future.successful(
      AuthenticatedOptionalDataRequest(
        request.request,
        request.credentials,
        vrn,
        request.iossNumber,
        dataToReturn
      ))
}

class FakeSavedAnswersRetrievalActionProvider(dataToReturn: Option[UserAnswers], vrn: Vrn)
  extends SavedAnswersRetrievalActionProvider(mockSessionRepository, mockSaveForLaterConnector)(ExecutionContext.Implicits.global){

  override def apply(): SavedAnswersRetrievalAction = new FakeSavedAnswersRetrievalAction(dataToReturn, vrn)

}

object FakeSavedAnswersRetrievalAction {
  val mockSessionRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]
}