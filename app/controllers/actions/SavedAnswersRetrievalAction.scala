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
import connectors.SaveForLaterHttpParser.SaveForLaterResponse
import models.UserAnswers
import models.requests.AuthenticatedOptionalDataRequest
import pages.SavedProgressPage
import play.api.mvc.ActionTransformer
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedAnswersRetrievalAction(repository: AuthenticatedUserAnswersRepository, saveForLaterConnector: SaveForLaterConnector)
                                 (implicit val executionContext: ExecutionContext)
  extends ActionTransformer[AuthenticatedOptionalDataRequest, AuthenticatedOptionalDataRequest] {

  override protected def transform[A](request: AuthenticatedOptionalDataRequest[A]): Future[AuthenticatedOptionalDataRequest[A]] = {
    val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
    val userAnswers: Future[Option[UserAnswers]] =
      if (request.userAnswers.flatMap(_.get(SavedProgressPage)).isEmpty) {
        for {
          savedForLater: SaveForLaterResponse <- saveForLaterConnector.get()(hc)
        } yield {
          val answers = {
            savedForLater match {
              case Right(Some(answers)) =>
                val newAnswers = UserAnswers(request.userId, answers.data, answers.vatInfo, answers.lastUpdated)
                repository.set(newAnswers)
                Some(newAnswers)

              case _ => request.userAnswers
            }
          }
          answers
        }
      } else {
        Future.successful(request.userAnswers)
      }

    userAnswers.map { (maybeUserAnswers: Option[UserAnswers]) =>
      AuthenticatedOptionalDataRequest(
        request.request,
        request.credentials,
        request.vrn,
        request.enrolments,
        request.iossNumber,
        maybeUserAnswers,
        request.numberOfIossRegistrations,
        request.latestOssRegistration
      )
    }
  }
}

class SavedAnswersRetrievalActionProvider @Inject()(repository: AuthenticatedUserAnswersRepository,
                                                    saveForLaterConnector: SaveForLaterConnector)
                                                   (implicit ec: ExecutionContext) {

  def apply(): SavedAnswersRetrievalAction =
    new SavedAnswersRetrievalAction(repository, saveForLaterConnector)
}
