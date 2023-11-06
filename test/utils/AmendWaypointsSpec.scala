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

import base.SpecBase
import cats.data.NonEmptyList
import models.{CheckMode, NormalMode}
import pages.amend.AmendYourAnswersPage
import pages.tradingNames.AddTradingNamePage
import pages.{NonEmptyWaypoints, Waypoint}
import utils.AmendWaypoints.AmendWaypointsOps

class AmendWaypointsSpec extends SpecBase {

  "must return true if waypoints contains amend-your-answers url fragment" in {

    val waypoint: Waypoint = Waypoint(AmendYourAnswersPage, CheckMode, AmendYourAnswersPage.urlFragment)
    val waypoints = NonEmptyWaypoints(NonEmptyList(waypoint, List.empty))

    implicit val amendWaypointsOps: AmendWaypointsOps = AmendWaypointsOps(waypoints)

    val result = amendWaypointsOps.inAmend

    result mustBe true
  }

  "must return false if waypoints does not contain amend-your-answers url fragment" in {

    val waypoint: Waypoint = Waypoint(AddTradingNamePage(), NormalMode, AddTradingNamePage().normalModeUrlFragment)
    val waypoints = NonEmptyWaypoints(NonEmptyList(waypoint, List.empty))

    implicit val amendWaypointsOps: AmendWaypointsOps = AmendWaypointsOps(waypoints)

    val result = amendWaypointsOps.inAmend

    result mustBe false
  }
}
