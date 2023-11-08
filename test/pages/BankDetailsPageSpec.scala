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

import base.SpecBase
import controllers.routes
import models.{BankDetails, Bic, CheckMode, Iban}
import org.scalacheck.Arbitrary.arbitrary
import pages.amend.ChangeRegistrationPage

class BankDetailsPageSpec extends SpecBase with PageBehaviours {

  private val genBic = arbitrary[Bic].sample.value
  private val genIban = arbitrary[Iban].sample.value
  private val bankDetails = BankDetails("account name", Some(genBic), genIban)
  private val userAnswers = basicUserAnswersWithVatInfo.set(BankDetailsPage, bankDetails).success.value

  "BankDetailsPage" - {

    beRetrievable[BankDetails](BankDetailsPage)

    beSettable[BankDetails](BankDetailsPage)

    beRemovable[BankDetails](BankDetailsPage)

    "must navigate in Normal mode" - {

      "to Check Your Answers" in {
        BankDetailsPage.navigate(EmptyWaypoints, userAnswers, userAnswers).route
          .mustEqual(routes.CheckYourAnswersController.onPageLoad())
      }
    }

    "must navigate in Check mode" - {

      "to Check Your Answers" in {
        val checkWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        BankDetailsPage.navigate(checkWaypoints, userAnswers, userAnswers).route
          .mustEqual(routes.CheckYourAnswersController.onPageLoad())
      }
    }

    "must navigate in Amend mode" - {

      "to Change Your Registration" in {
        val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
        BankDetailsPage.navigate(amendWaypoints, userAnswers, userAnswers).route
          .mustEqual(controllers.amend.routes.ChangeRegistrationController.onPageLoad())
      }
    }

  }
}
