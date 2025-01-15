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

import models.{Country, Index, UserAnswers}
import pages.euDetails.{AddEuDetailsPage, CheckEuDetailsAnswersPage, DeleteEuDetailsPage}
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.euDetails.{AllEuDetailsQuery, AllEuOptionalDetailsQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import viewmodels.ListItemWrapper
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object EuDetailsSummary {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints): Seq[ListItemWrapper] = {
    answers.get(AllEuOptionalDetailsQuery).getOrElse(List.empty).zipWithIndex.map {
      case (details, index) =>
        ListItemWrapper(
          ListItem(
            name = HtmlFormat.escape(details.euCountry.name).toString,
            changeUrl = controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, Index(index)).url,
            removeUrl = controllers.euDetails.routes.DeleteEuDetailsController.onPageLoad(waypoints, Index(index)).url
          ),
          removeButtonEnabled = true
        )
    }
  }

  def countryAndVatNumberList(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddItemPage)
                             (implicit messages: Messages): SummaryList =
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
              ActionItemViewModel("site.remove", DeleteEuDetailsPage(Index(countryIndex)).route(waypoints).url)
                .withVisuallyHiddenText(messages("euDetails.remove.hidden", euDetails.euCountry.name))
            ),
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )

  def checkAnswersRow(answers: UserAnswers, waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                     (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllEuDetailsQuery).map {
      euDetails =>

        val value = euDetails.map {
          details =>
            HtmlFormat.escape(details.euCountry.name)
        }.mkString("<br/>")

        val listRowViewModel = SummaryListRowViewModel(
          key = "euDetails.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = if (isCurrentIossAccount) {
            Seq(
              ActionItemViewModel("site.change", AddEuDetailsPage().changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages("euDetails.change.hidden"))
            )
          } else {
            Nil
          }
        )

        if (isCurrentIossAccount) {
          listRowViewModel
        } else {
          listRowViewModel.withCssClass("govuk-summary-list__row--no-actions")
        }
    }

  def amendedAnswersRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllEuDetailsQuery).map {
      euDetails =>

        val value = euDetails.map {
          details =>
            HtmlFormat.escape(details.euCountry.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = KeyViewModel("euDetails.checkYourAnswersLabel").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def removedAnswersRow(removedEuDetails: Seq[Country])(implicit messages: Messages): Option[SummaryListRow] =

    if (removedEuDetails.nonEmpty) {

      val value = removedEuDetails.map {
        details =>
          HtmlFormat.escape(details.name)
      }.mkString("<br/>")

      Some(
        SummaryListRowViewModel(
          key = KeyViewModel("euDetails.checkYourAnswersLabel.removed").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
      )
    } else {
      None
    }

  def changedAnswersRow(removedEuDetails: Seq[Country])(implicit messages: Messages): Option[SummaryListRow] =

    if (removedEuDetails.nonEmpty) {

      val value = removedEuDetails.map {
        details =>
          HtmlFormat.escape(details.name)
      }.mkString("<br/>")

      Some(
        SummaryListRowViewModel(
          key = KeyViewModel("euDetails.checkYourAnswersLabel.changed").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
      )
    } else {
      None
    }
}
