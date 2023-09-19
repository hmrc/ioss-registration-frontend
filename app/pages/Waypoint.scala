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

package pages

import models.{CheckMode, Index, Mode, NormalMode}
import pages.tradingNames.AddTradingNamePage
import pages.previousRegistrations.{AddPreviousRegistrationPage, CheckPreviousSchemeAnswersPage}

case class Waypoint(
                     page: WaypointPage,
                     mode: Mode,
                     urlFragment: String
                   )

object Waypoint {

  private val fragments: Map[String, Waypoint] =
    Map(
      AddTradingNamePage().normalModeUrlFragment -> AddTradingNamePage().waypoint(NormalMode),
      AddTradingNamePage().checkModeUrlFragment -> AddTradingNamePage().waypoint(CheckMode),
      // Continue journey
      CheckYourAnswersPage.urlFragment -> CheckYourAnswersPage.waypoint,
      AddPreviousRegistrationPage().normalModeUrlFragment -> AddPreviousRegistrationPage().waypoint(NormalMode),
      AddPreviousRegistrationPage().checkModeUrlFragment -> AddPreviousRegistrationPage().waypoint(CheckMode),
      CheckPreviousSchemeAnswersPage(Some(Index(0))).normalModeUrlFragment -> CheckPreviousSchemeAnswersPage(Some(Index(0))).waypoint(NormalMode),
      CheckPreviousSchemeAnswersPage(Some(Index(0))).checkModeUrlFragment -> CheckPreviousSchemeAnswersPage(Some(Index(0))).waypoint(CheckMode),
      CheckYourAnswersPage.urlFragment -> CheckYourAnswersPage.waypoint
    )

  def fromString(s: String): Option[Waypoint] =
    fragments.get(s)
}
