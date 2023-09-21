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
import pages.euDetails.RegistrationTypePage
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object RegistrationTypeSummary  {

  def row(answers: UserAnswers, waypoints: Waypoints, countryIndex: Index, pageSource: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegistrationTypePage(countryIndex)).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"registrationType.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "registrationType.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", RegistrationTypePage(countryIndex).changeLink(waypoints, pageSource).url)
              .withVisuallyHiddenText(messages("registrationType.change.hidden"))
          )
        )
    }
}
