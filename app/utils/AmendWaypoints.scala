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

import controllers.actions.{AmendingActiveRegistration, NotModifyingExistingRegistration, RegistrationModificationMode, RejoiningRegistration}
import pages.amend.ChangeRegistrationPage
import pages.rejoin.RejoinRegistrationPage
import pages.{CheckAnswersPage, CheckYourAnswersPage, NonEmptyWaypoints, Waypoints}

object AmendWaypoints {

  implicit class AmendWaypointsOps(waypoints: Waypoints) {

    def registrationModificationMode: RegistrationModificationMode =
      if (isInMode(RejoinRegistrationPage)) {
        RejoiningRegistration
      } else if( isInMode(ChangeRegistrationPage))  {
        AmendingActiveRegistration
      }else {
        NotModifyingExistingRegistration
      }

    def inAmend: Boolean = {
      isInMode(ChangeRegistrationPage, RejoinRegistrationPage)
    }

    private def isInMode(pages: CheckAnswersPage*) = {
      waypoints match {
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          pages.exists(page => nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(page.urlFragment))

        case _ =>
          false
      }
    }

    def inCheck: Boolean = {
      isInMode(CheckYourAnswersPage)
    }

    def getNextCheckYourAnswersPageFromWaypoints: Option[CheckAnswersPage] = {
      waypoints match {
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          List(RejoinRegistrationPage, ChangeRegistrationPage, CheckYourAnswersPage).find { page =>
            nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(page.urlFragment)
          }

        case _ =>
          None
      }
    }
  }

}
