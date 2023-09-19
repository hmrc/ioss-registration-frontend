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

import models.{CheckMode, Index, NormalMode}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.previousRegistrations.{AddPreviousRegistrationPage, CheckPreviousSchemeAnswersPage}
import pages.tradingNames.AddTradingNamePage
import pages.previousRegistrations.{AddPreviousRegistrationPage, CheckPreviousSchemeAnswersPage}

class WaypointSpec extends AnyFreeSpec with Matchers with OptionValues {

  "must return Add Trading Name when given it's Normal mode waypoint" in {

    Waypoint.fromString("add-uk-trading-name").value mustBe AddTradingNamePage().waypoint(NormalMode)
  }

  "must return Add Trading Name when given it's Check mode waypoint" in {

    Waypoint.fromString("change-add-uk-trading-name").value mustBe AddTradingNamePage().waypoint(CheckMode)
  }

  "must return Check Your Answers when given its waypoint" in {

    Waypoint.fromString("check-your-answers").value mustBe CheckYourAnswersPage.waypoint
  }

  "must return Add Previous Registration when given it's Normal mode waypoint" in {

    Waypoint.fromString("previous-schemes-overview").value mustBe AddPreviousRegistrationPage().waypoint(NormalMode)
  }

  "must return Add Previous Registration when given it's Check mode waypoint" in {
    Waypoint.fromString("change-previous-schemes-overview").value mustBe AddPreviousRegistrationPage().waypoint(CheckMode)
  }

  "must return Check Previous Scheme Answers when given it's Normal mode waypoint" in {

    Waypoint.fromString("previous-scheme-answers").value mustBe CheckPreviousSchemeAnswersPage(Some(Index(0))).waypoint(NormalMode)
  }

  "must return Check Previous Scheme Answers given it's Check mode waypoint" in {
    Waypoint.fromString("change-previous-scheme-answers").value mustBe CheckPreviousSchemeAnswersPage(Some(Index(0))).waypoint(CheckMode)
  }

  "must return check your answers when given its waypoint" in {
    Waypoint.fromString("check-your-answers").value mustBe CheckYourAnswersPage.waypoint
  }
}
