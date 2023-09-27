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

package pages.euDetails

import controllers.euDetails.routes
import models.{Index, UserAnswers}
import pages.{CheckAnswersPage, Page, Waypoint, Waypoints}
import play.api.mvc.Call

final case class CheckEuDetailsAnswersPage(countryIndex: Index) extends CheckAnswersPage {

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckEuDetailsAnswersPage => p.countryIndex == this.countryIndex
    case _ => false
  }

  override val urlFragment: String = s"check-tax-details-${countryIndex.display}"

  override def route(waypoints: Waypoints): Call =
    routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    AddEuDetailsPage(Some(countryIndex)) // TODO -> to Websites
}

object CheckEuDetailsAnswersPage {

  def waypointFromString(s: String): Option[Waypoint] = {

    val pattern = """check-tax-details-(\d{1,3})""".r.anchored // TODO -> Check /d{1,3}

    s match {
      case pattern(indexDisplay) =>
        Some(CheckEuDetailsAnswersPage(Index(indexDisplay.toInt - 1)).waypoint)

      case _ => None
    }
  }
}
