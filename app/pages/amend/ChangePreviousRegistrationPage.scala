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

package pages.amend

import controllers.amend.{routes => amendRoutes}
import models.UserAnswers
import pages.{CheckAnswersPage, Page, Waypoints}
import play.api.mvc.Call

object ChangePreviousRegistrationPage extends CheckAnswersPage {

  override def isTheSamePage(other: Page): Boolean = other match {
    case ChangeRegistrationPage => true
    case _ => false
  }

  override val urlFragment: String = "change-a-previous-registration"

  override def route(waypoints: Waypoints): Call =
    amendRoutes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    AmendCompletePage
}
