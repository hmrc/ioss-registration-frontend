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
import pages.{CheckAnswersPage, Waypoints}
import pages.euDetails.HasFixedEstablishmentPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object HasFixedEstablishmentSummary {

  def row(
           answers: UserAnswers,
           country: Country,
           waypoints: Waypoints,
           countryIndex: Index,
           sourcePage: CheckAnswersPage
         )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasFixedEstablishmentPage(countryIndex)).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key = "hasFixedEstablishment.checkYourAnswersLabel",
          value = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", HasFixedEstablishmentPage(countryIndex).changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("hasFixedEstablishment.change.hidden", country))
          )
        )
    }

  def amendedAnswersRow(
           answers: UserAnswers,
           countryIndex: Index
         )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasFixedEstablishmentPage(countryIndex)).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key = "hasFixedEstablishment.checkYourAnswersLabel",
          value = ValueViewModel(value),
        )
    }
}
