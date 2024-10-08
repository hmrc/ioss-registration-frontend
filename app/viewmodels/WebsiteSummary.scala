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

package viewmodels

import models.{Index, UserAnswers}
import pages.website.{AddWebsitePage, DeleteWebsitePage, WebsitePage}
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.AllWebsites
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object WebsiteSummary {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddWebsitePage): Seq[ListItemWrapper] =
    answers.get(AllWebsites).getOrElse(List.empty).zipWithIndex.map {
      case (website, index) =>
        ListItemWrapper(
          ListItem(
            name = HtmlFormat.escape(website.site).toString,
            changeUrl = WebsitePage(Index(index)).changeLink(waypoints, sourcePage).url,
            removeUrl = DeleteWebsitePage(Index(index)).route(waypoints).url
          ),
          removeButtonEnabled = true
        )
    }

  def checkAnswersRow(
                       answers: UserAnswers,
                       waypoints: Waypoints,
                       sourcePage: CheckAnswersPage,
                       isCurrentIossAccount: Boolean
                     )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllWebsites).map {
      websites =>

        val value = websites.map {
          website =>
            HtmlFormat.escape(website.site)
        }.mkString("<br/>")

        val addWebsitePageChangeUrl = AddWebsitePage().changeLink(waypoints, sourcePage).url

        val listRowViewModel = SummaryListRowViewModel(
          key = "websites.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = if (isCurrentIossAccount) {
            Seq(
              ActionItemViewModel("site.change", addWebsitePageChangeUrl)
                .withVisuallyHiddenText(messages("websites.change.hidden"))
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
    answers.get(AllWebsites).map {
      websites =>

        val value = websites.map {
          website =>
            HtmlFormat.escape(website.site)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = KeyViewModel("websites.checkYourAnswersLabel.added").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def removedAnswersRow(removedWebsites: Seq[String])(implicit messages: Messages): Option[SummaryListRow] =

    if (removedWebsites.nonEmpty) {
        val value = removedWebsites.map {
          website =>
            HtmlFormat.escape(website)
        }.mkString("<br/>")

      Some(
        SummaryListRowViewModel(
          key = KeyViewModel("websites.checkYourAnswersLabel.removed").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
      )
    } else {
      None
    }
}
