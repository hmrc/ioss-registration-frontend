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

package viewmodels.checkAnswers.previousRegistrations

import models.{Country, Index, PreviousScheme, UserAnswers}
import pages.Waypoints
import pages.previousRegistrations.PreviousSchemePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousSchemeSummary  {

  def row(answers: UserAnswers, countryIndex: Index, schemeIndex: Index, country: Country, existingPreviousSchemes: Seq[PreviousScheme], waypoints: Waypoints)
         (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PreviousSchemePage(countryIndex, schemeIndex)).map {
      previousAnsweredScheme: PreviousScheme =>

        val isExistingScheme = existingPreviousSchemes.contains(previousAnsweredScheme)

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"previousScheme.$previousAnsweredScheme"))
          )
        )

        SummaryListRowViewModel(
          key     = "previousScheme.checkYourAnswersLabel",
          value   = value,
          actions = if (!isExistingScheme) {
            Seq(
              ActionItemViewModel("site.remove", controllers.previousRegistrations.routes.DeletePreviousSchemeController.onPageLoad(
                waypoints, countryIndex, schemeIndex).url)
                .withVisuallyHiddenText(messages("site.remove.hidden", country.name, HtmlFormat.escape(messages(s"previousScheme.$previousAnsweredScheme"))))
            )
          } else {
            Seq.empty
          }
        )
    }
}
