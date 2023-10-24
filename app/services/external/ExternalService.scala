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

package services.external

import logging.Logging
import models.SessionData
import models.external.{ExternalRequest, ExternalResponse}
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExternalService @Inject()(sessionRepository: SessionRepository)(implicit executionContext: ExecutionContext) extends Logging {

  def getExternalResponse(externalRequest: ExternalRequest, userId: String): Future[ExternalResponse] = {
    for {
      _ <- saveReturnUrl(userId, externalRequest)
    } yield {
      ExternalResponse(controllers.routes.IndexController.onPageLoad().url)
    }
  }

  private def saveReturnUrl(userId: String, externalRequest: ExternalRequest): Future[Boolean] = {
    for {
      sessionData <- sessionRepository.get(userId)
      updatedData <- Future.fromTry(sessionData.headOption.getOrElse(SessionData(userId)).set(ExternalReturnUrlQuery.path, externalRequest.returnUrl))
      savedData <- sessionRepository.set(updatedData)
    } yield {
      savedData
    }
  }.recover{
    case e: Exception =>
      logger.error(s"An error occurred while saving the external returnUrl in the session, ${e.getMessage}")
      false
  }
}