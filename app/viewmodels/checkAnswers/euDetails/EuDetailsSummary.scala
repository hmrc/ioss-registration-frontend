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

package viewmodels.checkAnswers.euDetails

import models.{Index, UserAnswers}
import pages.euDetails.{AddEuDetailsPage, CheckEuDetailsAnswersPage}
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import viewmodels.ListItemWrapper
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object EuDetailsSummary {

//  def addToListRows(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddItemPage): Seq[ListItemWrapper] =
//    answers.get(AllEuDetailsQuery).getOrElse(List.empty).zipWithIndex.map {
//      case (euDetails, countryIndex) =>
//
//        ListItemWrapper(
//          ListItem(
//            name = HtmlFormat.escape(euDetails.euCountry.name).toString,
//            changeUrl = CheckEuDetailsAnswersPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url,
//            removeUrl = ???
//          ),
//          removeButtonEnabled = true
//        )
//    }


  def checkAnswersRow(answers: UserAnswers, waypoints: Waypoints, sourcePage: CheckAnswersPage)
                     (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllEuDetailsQuery).map {
      euDetails =>

        val value = euDetails.map {
          details =>
            HtmlFormat.escape(details.euCountry.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = "euDetails.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", AddEuDetailsPage().changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("euDetails.change.hidden"))
          )
        )
    }

  def countryAndVatNumberList(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddItemPage)
                             (implicit messages: Messages): SummaryList = {

    SummaryList(
      answers.get(AllEuDetailsQuery).getOrElse(List.empty).zipWithIndex.map {
        case (euDetails, countryIndex) =>

          val value = euDetails.euVatNumber.getOrElse("") + euDetails.euTaxReference.getOrElse("")

          SummaryListRowViewModel(
            key = euDetails.euCountry.name,
            value = ValueViewModel(HtmlContent(value)),
            actions = Seq(
              ActionItemViewModel("site.change", CheckEuDetailsAnswersPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("change.euDetails.hidden", euDetails.euCountry.name)),
              ActionItemViewModel("site.remove", ???)
                .withVisuallyHiddenText(messages("euDetails.remove.hidden", euDetails.euCountry.name))
            ),
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )
  }
}
