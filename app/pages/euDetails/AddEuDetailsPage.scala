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
import models.{Country, Index, UserAnswers}
import pages.website.WebsitePage
import pages.{AddItemPage, Page, QuestionPage, Waypoints,RecoveryOps}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.euDetails.DeriveNumberOfEuRegistrations

object AddEuDetailsPage{
   val normalModeUrlFragment: String = "add-tax-details"
   val checkModeUrlFragment: String = "change-add-tax-details"
}

final case class AddEuDetailsPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddEuDetailsPage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = AddEuDetailsPage.normalModeUrlFragment
  override val checkModeUrlFragment: String =  AddEuDetailsPage.checkModeUrlFragment

  override def path: JsPath = JsPath \ toString

  override def toString: String = "addEuDetails"

  override def route(waypoints: Waypoints): Call =
    routes.AddEuDetailsController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        index
          .map { i =>
            if (i.position + 1 < Country.euCountries.size) {
              EuCountryPage(Index(i.position + 1))
            } else {
              WebsitePage(Index(0))
            }
          }
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => EuCountryPage(Index(n)))
              .orRecover
          }
      case false => WebsitePage(Index(0))
    }.orRecover

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfEuRegistrations
}
