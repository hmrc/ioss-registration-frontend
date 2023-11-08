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

import models.{Index, NormalMode, UserAnswers, Website}
import pages.{AddToListQuestionPage, AddToListSection, NonEmptyWaypoints, Page, QuestionPage, Waypoint, Waypoints, WebsiteSection}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllWebsites

import scala.util.Try

case class WebsitePage(index: Index) extends QuestionPage[Website] with AddToListQuestionPage {

  override val section: AddToListSection = WebsiteSection
  override def path: JsPath = JsPath \ "websites" \ index.position

  override val addItemWaypoint: Waypoint = AddWebsitePage().waypoint(NormalMode)

  override def route(waypoints: Waypoints): Call =
    controllers.website.routes.WebsiteController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    AddWebsitePage(None)

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    AddWebsitePage(None)

  override def cleanup(value: Option[Website], userAnswers: UserAnswers): Try[UserAnswers] = {
    if (userAnswers.get(AllWebsites).exists(_.isEmpty)) {
      userAnswers.remove(AllWebsites)
    } else {
      super.cleanup(value, userAnswers)
    }
  }
}
