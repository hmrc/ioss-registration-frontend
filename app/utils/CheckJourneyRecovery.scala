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

package utils

import controllers.routes
import controllers.amend.{routes => amendRoutes}
import pages.Waypoints
import play.api.mvc.Call
import utils.AmendWaypoints.AmendWaypointsOps

object CheckJourneyRecovery {

  def determineJourneyRecovery(waypoints: Waypoints): Call = {
    if (waypoints.inAmend) {
      amendRoutes.AmendJourneyRecoveryController.onPageLoad()
    } else {
      routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
