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

import models.{CheckMode, Mode, NormalMode}
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.euDetails.{AddEuDetailsPage, CheckEuDetailsAnswersPage}
import pages.tradingNames.AddTradingNamePage
import pages.previousRegistrations.{AddPreviousRegistrationPage, CheckPreviousSchemeAnswersPage}
import pages.rejoin.RejoinRegistrationPage
import pages.website.AddWebsitePage

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
      AddPreviousRegistrationPage().normalModeUrlFragment -> AddPreviousRegistrationPage().waypoint(NormalMode),
      AddPreviousRegistrationPage().checkModeUrlFragment -> AddPreviousRegistrationPage().waypoint(CheckMode),
      AddEuDetailsPage().normalModeUrlFragment -> AddEuDetailsPage().waypoint(NormalMode),
      AddEuDetailsPage().checkModeUrlFragment -> AddEuDetailsPage().waypoint(CheckMode),
      AddWebsitePage().normalModeUrlFragment ->  AddWebsitePage().waypoint(NormalMode),
      AddWebsitePage().checkModeUrlFragment ->  AddWebsitePage().waypoint(CheckMode),
      // Continue journey
      CheckYourAnswersPage.urlFragment -> CheckYourAnswersPage.waypoint,
      ChangeRegistrationPage.urlFragment -> ChangeRegistrationPage.waypoint,
      ChangePreviousRegistrationPage.urlFragment -> ChangePreviousRegistrationPage.waypoint,
      RejoinRegistrationPage.urlFragment -> RejoinRegistrationPage.waypoint
    )

  def fromString(s: String): Option[Waypoint] =
    fragments.get(s)
      .orElse(CheckEuDetailsAnswersPage.waypointFromString(s))
      .orElse(CheckPreviousSchemeAnswersPage.waypointFromString(s))
}
