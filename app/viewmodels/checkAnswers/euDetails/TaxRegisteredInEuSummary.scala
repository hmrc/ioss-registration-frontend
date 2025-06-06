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

import models.UserAnswers
import pages.euDetails.TaxRegisteredInEuPage
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TaxRegisteredInEuSummary  {

  def row(
           answers: UserAnswers,
           waypoints: Waypoints,
           sourcePage: CheckAnswersPage,
           isCurrentIossAccount: Boolean
         )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TaxRegisteredInEuPage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "taxRegisteredInEu.mini.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = if (isCurrentIossAccount) {
            Seq(
              ActionItemViewModel("site.change", TaxRegisteredInEuPage.changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages("taxRegisteredInEu.change.hidden"))
            )
          } else {
            Nil
          }
        )
    }

  def checkAnswersRow(answers: UserAnswers, waypoints: Waypoints, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TaxRegisteredInEuPage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "taxRegisteredInEu.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", TaxRegisteredInEuPage.changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("taxRegisteredInEu.change.hidden"))
          )
        )
    }

  def amendedRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TaxRegisteredInEuPage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = KeyViewModel("taxRegisteredInEu.mini.checkYourAnswersLabel").withCssClass("govuk-!-width-one-half"),
          value   = ValueViewModel(value)
        )
    }
}
