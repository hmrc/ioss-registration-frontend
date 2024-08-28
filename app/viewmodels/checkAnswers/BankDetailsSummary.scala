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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.{BankDetailsPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object BankDetailsSummary {

  def rowAccountName(answers: UserAnswers,
                     waypoints: Waypoints,
                     sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = HtmlFormat.escape(answer.accountName).toString

        SummaryListRowViewModel(
          key = "bankDetails.accountName",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", createBusinessDetailsChangeLinkUrl(waypoints, sourcePage))
              .withVisuallyHiddenText(messages("bankDetails.change.accountName.hidden"))
          )
        )
    }

  private def createBusinessDetailsChangeLinkUrl(waypoints: Waypoints,
                                                 sourcePage: CheckAnswersPage): String = {
    BankDetailsPage.changeLink(waypoints, sourcePage).url
  }

  def rowBIC(answers: UserAnswers,
             waypoints: Waypoints,
             sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = Seq(
          answer.bic.map(bic => HtmlFormat.escape(bic.toString))
        ).flatten.mkString

        SummaryListRowViewModel(
          key = "bankDetails.bic",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", createBusinessDetailsChangeLinkUrl(waypoints, sourcePage))
              .withVisuallyHiddenText(messages("bankDetails.change.bic.hidden"))
          )
        )
    }

  def rowIBAN(answers: UserAnswers,
              waypoints: Waypoints,
              sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = HtmlFormat.escape(answer.iban.toString).toString

        SummaryListRowViewModel(
          key = "bankDetails.iban",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", createBusinessDetailsChangeLinkUrl(waypoints, sourcePage))
              .withVisuallyHiddenText(messages("bankDetails.change.iban.hidden"))
          )
        )
    }

  def amendedRowAccountName(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = HtmlFormat.escape(answer.accountName).toString

        SummaryListRowViewModel(
          key = KeyViewModel("bankDetails.accountName").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def amendedRowBIC(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = Seq(
          answer.bic.map(bic => HtmlFormat.escape(bic.toString))
        ).flatten.mkString

        SummaryListRowViewModel(
          key = KeyViewModel("bankDetails.bic").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def amendedRowIBAN(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(BankDetailsPage).map {
      answer =>
        val value = HtmlFormat.escape(answer.iban.toString).toString

        SummaryListRowViewModel(
          key = KeyViewModel("bankDetails.iban").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }
}

