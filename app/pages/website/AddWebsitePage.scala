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

package pages.website

import controllers.website
import models.{Index, UserAnswers}
import pages.{AddItemPage, BusinessContactDetailsPage, JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfWebsites}


object AddWebsitePage {
  val normalModeUrlFragment: String = "add-website-address"
  val checkModeUrlFragment: String = "change-add-website-address"
}

final case class AddWebsitePage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ "addWebsite"

  override val normalModeUrlFragment: String = AddWebsitePage.normalModeUrlFragment
  override val checkModeUrlFragment: String = AddWebsitePage.checkModeUrlFragment

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddWebsitePage => true
    case _ => false
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    navigate(answers, nextPage = BusinessContactDetailsPage)
  }

  private def navigate(answers: UserAnswers, nextPage: Page): Page = {
    (answers.get(AddWebsitePage()), answers.get(DeriveNumberOfWebsites)) match {
      case (Some(true), Some(size)) => if (size < config.Constants.maxWebsites) {
        WebsitePage(Index(size))
      } else {
        nextPage
      }
      case (Some(false), _) => nextPage
      case _ => JourneyRecoveryPage
    }
  }

  // TODO -> Needed/Remove?
//  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
//    navigate(answers, nextPage = CheckYourAnswersPage)
//  }

  override def route(waypoints: Waypoints): Call =
    website.routes.AddWebsiteController.onPageLoad(waypoints)


  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfWebsites
}
