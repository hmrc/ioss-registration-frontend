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

import connectors.{SaveForLaterConnector, SavedUserAnswers}
import logging.Logging
import models.requests.{AuthenticatedDataRequest, SaveForLaterRequest}
import pages.{JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterService @Inject()(
                                     sessionRepository: AuthenticatedUserAnswersRepository,
                                     saveForLaterConnector: SaveForLaterConnector
                                   ) extends Logging {

  def saveAnswers(
                   waypoints: Waypoints,
                   redirectLocation: Call,
                   originLocation: Call
                 )(implicit request: AuthenticatedDataRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    saveAnswersRedirect(waypoints, redirectLocation.url, originLocation.url)
  }


  def saveAnswersRedirect(
                           waypoints: Waypoints,
                           redirectLocation: String,
                           originLocation: String
                         )(implicit request: AuthenticatedDataRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    logger.info("saving answers")
    Future.fromTry(request.userAnswers.set(SavedProgressPage, originLocation)).flatMap {
      updatedAnswers =>
        val save4LaterRequest = SaveForLaterRequest(updatedAnswers.data, request.vrn)
        saveForLaterConnector.submit(save4LaterRequest).flatMap {
          case Right(Some(_: SavedUserAnswers)) =>
            for {
              _ <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(redirectLocation)
            }
          case Right(None) =>
            logger.error(s"Unexpected result on submit")
            Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
          case Left(e) =>
            logger.error(s"Unexpected result on submit: ${e.toString}")
            Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
        }
    }
  }
}
