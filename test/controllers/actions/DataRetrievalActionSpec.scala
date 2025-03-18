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
import models.UserAnswers
import models.requests.{AuthenticatedIdentifierRequest, AuthenticatedOptionalDataRequest, SessionRequest, UnauthenticatedOptionalDataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.{AuthenticatedUserAnswersRepository, UnauthenticatedUserAnswersRepository}
import services.DataMigrationService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderNames

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(sessionRepository: UnauthenticatedUserAnswersRepository) extends UnauthenticatedDataRetrievalAction(sessionRepository) {
    def callTransform[A](request: SessionRequest[A]): Future[UnauthenticatedOptionalDataRequest[A]] = transform(request)
  }

  class AuthenticatedHarness(
                              sessionRepository: AuthenticatedUserAnswersRepository,
                              migrationService: DataMigrationService
                            ) extends AuthenticatedDataRetrievalAction(sessionRepository, migrationService) {
    def callRefine[A](request: AuthenticatedIdentifierRequest[A]): Future[Either[Result, AuthenticatedOptionalDataRequest[A]]] = refine(request)
  }

  "Unauthenticated Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {

        val sessionRepository = mock[UnauthenticatedUserAnswersRepository]
        when(sessionRepository.get("id")) thenReturn Future(None)
        val action = new Harness(sessionRepository)

        val result = action.callTransform(SessionRequest(fakeRequest, "id")).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        val sessionRepository = mock[UnauthenticatedUserAnswersRepository]
        when(sessionRepository.get("id")) thenReturn Future(Some(UserAnswers("id")))
        val action = new Harness(sessionRepository)

        val result = action.callTransform(SessionRequest(fakeRequest, "id")).futureValue

        result.userAnswers mustBe defined
      }
    }

    "when a key is provided in the querystring" - {

      "must migrate the session then redirect the user to the same path without the key" in {

        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val migrationService = mock[DataMigrationService]

        when(sessionRepository.get(userAnswersId)) thenReturn Future.successful(None)
        when(migrationService.migrate(any(), any())) thenReturn Future.successful(UserAnswers(userAnswersId))

        val action = new AuthenticatedHarness(sessionRepository, migrationService)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(AuthenticatedIdentifierRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), 1, None)).futureValue

        verify(migrationService, times(1)).migrate("session-id", userAnswersId)
        result mustBe Left(Redirect("/test/url"))
      }
    }

    "when no key is provided in the querystring" - {

      "and there is no data in the authenticated repository" - {

        "must migrate data from the authenticated repository for the current session id" in {

          val answers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"))

          val sessionRepository = mock[AuthenticatedUserAnswersRepository]
          val migrationService = mock[DataMigrationService]

          when(sessionRepository.get(any())) thenReturn Future.successful(None)
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(migrationService.migrate(any(), any())) thenReturn Future.successful(answers)

          val sessionId = "session-id"
          val action = new AuthenticatedHarness(sessionRepository, migrationService)
          val request = FakeRequest(GET, "/test/url").withHeaders(HeaderNames.xSessionId -> sessionId)

          val result = action.callRefine(AuthenticatedIdentifierRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), 1, None)).futureValue

          verify(migrationService, times(1)).migrate("session-id", userAnswersId)
          result.value.credentials mustEqual testCredentials
          result.value.vrn mustEqual vrn
          result.value.userAnswers.value mustEqual answers
        }

        "must migrate data from the authenticated repository when no session id" in {

          val answers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"))

          val sessionRepository = mock[AuthenticatedUserAnswersRepository]
          val migrationService = mock[DataMigrationService]

          when(sessionRepository.get(any())) thenReturn Future.successful(None)
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(migrationService.migrate(any(), any())) thenReturn Future.successful(answers)

          val action = new AuthenticatedHarness(sessionRepository, migrationService)
          val request = FakeRequest(GET, "/test/url")

          val result = action.callRefine(AuthenticatedIdentifierRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), 1, None)).futureValue

          verifyNoInteractions(migrationService)
          result.value.credentials mustEqual testCredentials
          result.value.vrn mustEqual vrn
          result.value.userAnswers mustBe None
        }
      }

      "and there is data in the authenticated repository" - {

        "must build a userAnswers object and add it to the request" in {

          val answers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"))

          val sessionRepository = mock[AuthenticatedUserAnswersRepository]
          val migrationService = mock[DataMigrationService]

          when(sessionRepository.get(any())) thenReturn Future.successful(Some(answers))

          val action = new AuthenticatedHarness(sessionRepository, migrationService)
          val request = FakeRequest(GET, "/test/url")

          val result = action.callRefine(AuthenticatedIdentifierRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), 1, None)).futureValue
          verify(migrationService, never()).migrate(any(), any())
          result.value.credentials mustEqual testCredentials
          result.value.vrn mustEqual vrn
          result.value.userAnswers.value mustEqual answers
        }
      }
    }

    "when latestOssRegistration is provided" - {

      "must include latestOssRegistration in the request" in {

        val answers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"))
        val latestOssRegistration = ossRegistration

        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val migrationService = mock[DataMigrationService]

        when(sessionRepository.get(any())) thenReturn Future.successful(Some(answers))

        val action = new AuthenticatedHarness(sessionRepository, migrationService)
        val request = FakeRequest(GET, "/test/url")

        val result = action.callRefine(AuthenticatedIdentifierRequest(
          request,
          testCredentials,
          vrn,
          Enrolments(Set.empty),
          Some(iossNumber),
          1,
          latestOssRegistration
        )).futureValue

        verify(migrationService, never()).migrate(any(), any())
        result.value.latestOssRegistration mustBe latestOssRegistration
      }
    }

    "when latestOssRegistration is not provided" - {

      "must keep latestOssRegistration as None" in {

        val answers = UserAnswers(userAnswersId, Json.obj("foo" -> "bar"))

        val sessionRepository = mock[AuthenticatedUserAnswersRepository]
        val migrationService = mock[DataMigrationService]

        when(sessionRepository.get(any())) thenReturn Future.successful(Some(answers))

        val action = new AuthenticatedHarness(sessionRepository, migrationService)
        val request = FakeRequest(GET, "/test/url")

        val result = action.callRefine(AuthenticatedIdentifierRequest(
          request,
          testCredentials,
          vrn,
          Enrolments(Set.empty),
          Some(iossNumber),
          1,
          None // No latestOssRegistration provided
        )).futureValue

        verify(migrationService, never()).migrate(any(), any())
        result.value.latestOssRegistration mustBe None
      }
    }
  }
}
