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

import base.SpecBase
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import models.UserAnswers
import models.requests.AuthenticatedOptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.AuthenticatedUserAnswersRepository

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SavedAnswersRetrievalActionSpec extends SpecBase with MockitoSugar with EitherValues {

  class Harness(
                 repository: AuthenticatedUserAnswersRepository,
                 saveForLaterConnector: SaveForLaterConnector,
                 clock: Clock
               ) extends SavedAnswersRetrievalAction(repository, saveForLaterConnector) {
    def callRefine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedOptionalDataRequest[A]]] = refine(request)
  }


  "Authenticated Data Retrieval Action" - {

    "when there is answers in session with continueUrl set" - {

      "must use the answers saved in session" in {

        val instant = Instant.now
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val connector = mock[SaveForLaterConnector]

        val answers = UserAnswers(userAnswersId).set(SavedProgressPage, "/url").success.value
        val action = new Harness(sessionRepository, connector, stubClock)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(
          AuthenticatedOptionalDataRequest(
            request = request,
            credentials = testCredentials,
            vrn = vrn,
            iossNumber = None,
            userAnswers = Some(answers))
        ).futureValue

        verifyNoInteractions(connector)
        verifyNoInteractions(sessionRepository)
        result.value.userAnswers mustBe (Some(answers))
      }
    }

    "when there is no answers in session with continueUrl set" - {
      "must retrieve saved answers when present" in {
        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val connector = mock[SaveForLaterConnector]
        val instant = Instant.now
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val answers = UserAnswers(userAnswersId, lastUpdated = Instant.now(stubClock)).set(SavedProgressPage, "/url").success.value

        when(connector.get()(any())) thenReturn Future.successful(Right(Some(SavedUserAnswers(vrn, answers.data, None, instant))))
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        val action = new Harness(sessionRepository, connector, stubClock)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(
          AuthenticatedOptionalDataRequest(
            request = request,
            credentials = testCredentials,
            vrn = vrn,
            iossNumber = None,
            userAnswers = Some(UserAnswers(userAnswersId))
          )
        ).futureValue

        verify(connector, times(1)).get()(any())
        result.value.userAnswers mustBe Some(answers)
      }

      "must use answers in request when no saved answers present" in {
        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val connector = mock[SaveForLaterConnector]
        val instant = Instant.now
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val emptyAnswers = UserAnswers(userAnswersId)

        when(connector.get()(any())) thenReturn Future.successful(Right(None))
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        val action = new Harness(sessionRepository, connector, stubClock)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(
          AuthenticatedOptionalDataRequest(request,
            credentials = testCredentials,
            vrn = vrn,
            iossNumber = None,
            userAnswers = Some(emptyAnswers))
        ).futureValue

        verify(connector, times(1)).get()(any())
        result.value.userAnswers mustBe Some(emptyAnswers)
      }
    }

  }

}
