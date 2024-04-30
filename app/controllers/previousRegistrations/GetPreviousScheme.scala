/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.previousRegistrations

import logging.Logging
import models.requests.AuthenticatedDataRequest
import models.{Index, PreviousScheme}
import pages.previousRegistrations.PreviousSchemePage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future


object GetPreviousScheme extends Logging {

  def getPreviousScheme(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index)
                               (block: PreviousScheme => Future[Result])
                               (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousSchemePage(countryIndex, schemeIndex)).map {
      previousScheme =>
        block(previousScheme)
    }.getOrElse {
      logger.error("Failed to get previous scheme")
      Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
    }
}
