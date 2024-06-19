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

package utils

import base.SpecBase
import controllers.amend.{routes => amendRoutes}
import controllers.routes
import models.{CheckMode, NormalMode}
import pages.amend.ChangeRegistrationPage
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import utils.CheckJourneyRecovery.determineJourneyRecovery

class CheckJourneyRecoverySpec extends SpecBase {

  "CheckJourneyRecovery" - {

    ".determineJourneyRecovery" - {

      "must determine correct Journey Recovery path when in Amend mode" in {

        val waypoints: Waypoints = EmptyWaypoints
          .setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

        val result = determineJourneyRecovery(waypoints)

        result mustBe amendRoutes.AmendJourneyRecoveryController.onPageLoad()
      }

      "must determine correct Journey Recovery path when not in Amend mode" in {

        val waypoints: Waypoints = EmptyWaypoints
          .setNextWaypoint(Waypoint(CheckYourAnswersPage, NormalMode, CheckYourAnswersPage.urlFragment))

        val result = determineJourneyRecovery(waypoints)

        result mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
