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

package pages.euDetails

import base.SpecBase
import controllers.euDetails.routes as euRoutes
import models.{CheckMode, Country, Index, NormalMode}
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}

class DeleteEuDetailsPageSpec extends SpecBase {

  "DeleteEuDetailsPage" - {

    "must navigate in Normal mode" - {

      "to Tax Registered in EU when there are no countries left in the user's answers" in {

        DeleteEuDetailsPage(Index(0)).navigate(EmptyWaypoints, emptyUserAnswers, emptyUserAnswers).route
          .mustEqual(euRoutes.TaxRegisteredInEuController.onPageLoad(EmptyWaypoints))
      }

      "to Add EU Details when we still have countries in the user's answers" in {

        val answers =
          emptyUserAnswers
            .set(pages.euDetails.EuCountryPage(Index(0)), Country("FR", "France")).success.value
            .set(pages.euDetails.EuVatNumberPage(Index(0)), "VAT Number").success.value

        DeleteEuDetailsPage(Index(0)).navigate(EmptyWaypoints, answers, answers).route
          .mustEqual(euRoutes.AddEuDetailsController.onPageLoad(EmptyWaypoints))
      }
    }

    "must navigate in Check mode" - {

      "to Tax Registered in EU when there are no countries left in the user's answers" in {
        val checkWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        DeleteEuDetailsPage(Index(0)).navigate(checkWaypoints, emptyUserAnswers, emptyUserAnswers).route
          .mustEqual(euRoutes.TaxRegisteredInEuController.onPageLoad(checkWaypoints))
      }

      "to Add EU Details when we still have countries in the user's answers" in {
        val checkWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        val answers =
          emptyUserAnswers
            .set(pages.euDetails.EuCountryPage(Index(0)), Country("FR", "France")).success.value
            .set(pages.euDetails.EuVatNumberPage(Index(0)), "VAT Number").success.value

        DeleteEuDetailsPage(Index(0)).navigate(checkWaypoints, answers, answers).route
          .mustEqual(euRoutes.AddEuDetailsController.onPageLoad(checkWaypoints))
      }
    }
  }
}
