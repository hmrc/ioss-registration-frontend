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

package services

import models.{SessionData, UserAnswers}
import repositories.{AuthenticatedUserAnswersRepository, SessionRepository, UnauthenticatedUserAnswersRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataMigrationService @Inject()(
                                      authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository,
                                      unauthenticatedUserAnswersRepository: UnauthenticatedUserAnswersRepository,
                                      sessionRepository: SessionRepository
                                    )(implicit executionContext: ExecutionContext) {
  def migrate(sessionId: String, userId: String): Future[UserAnswers] = {
    for {
      maybeAnswers <- unauthenticatedUserAnswersRepository.get(sessionId)
      answers = maybeAnswers.fold(UserAnswers(userId))(_.copy(id = userId))
      success <- authenticatedUserAnswersRepository.set(answers)
      sessionData <- sessionRepository.get(sessionId)
      updatedSessionData = sessionData.headOption.fold(SessionData(userId))(_.copy(userId = userId))
      _ <- sessionRepository.set(updatedSessionData)
    } yield if (success) {
      answers
    } else {
      throw DataOperationFailedError("Failed to set authenticated user answers during migration")
    }
  }
}

case class DataOperationFailedError(message: String) extends Exception(message)
