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

import models.{Index, PreviousScheme, UserAnswers}
import pages.previousRegistrations.PreviousOssNumberPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousSchemeNumberSummary {

  def row(answers: UserAnswers, countryIndex: Index, schemeIndex: Index, previousScheme: Option[PreviousScheme])
         (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PreviousOssNumberPage(countryIndex, schemeIndex)).map {
      answer =>
        val previousSchemeNumber: String = if (previousScheme.contains(PreviousScheme.OSSNU)) {
          "EU" + answer.previousSchemeNumber
        } else {
          answer.previousSchemeNumber
        }

        SummaryListRowViewModel(
          key = "previousSchemeNumber.checkYourAnswersLabel",
          value = ValueViewModel(HtmlFormat.escape(previousSchemeNumber).toString),
          actions = Seq()
        )
    }
}

