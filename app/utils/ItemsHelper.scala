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

package utils

import controllers.actions.AuthenticatedControllerComponents
import models.requests.AuthenticatedDataRequest
import pages.{JourneyRecoveryPage, QuestionPage, Waypoints}
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.Redirect
import queries.{Derivable, Settable}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

object ItemsHelper {

  def getDerivedItems(waypoints: Waypoints, derivable: Derivable[Seq[JsObject], Int])(block: Int => Future[Result])
                     (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    request.userAnswers.get(derivable).map {
      number =>
        block(number)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
  }

  def determineRemoveAllItemsAndRedirect[A](
                                             waypoints: Waypoints,
                                             value: Boolean,
                                             cc: AuthenticatedControllerComponents,
                                             query: Settable[A],
                                             hasItems: QuestionPage[Boolean],
                                             deleteAllItems: QuestionPage[Boolean]
                                           )(implicit ec: ExecutionContext, request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    val removeItems = if (value) {
      request.userAnswers.remove(query)
    } else {
      request.userAnswers.set(hasItems, true)
    }
    for {
      updatedAnswers <- Future.fromTry(removeItems)
      calculatedAnswers <- Future.fromTry(updatedAnswers.set(deleteAllItems, value))
      _ <- cc.sessionRepository.set(calculatedAnswers)
    } yield Redirect(deleteAllItems.navigate(waypoints, request.userAnswers, calculatedAnswers).route)
  }
}
