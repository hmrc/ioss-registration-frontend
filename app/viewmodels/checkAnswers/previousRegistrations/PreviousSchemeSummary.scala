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

import models.{Country, Index, PreviousScheme}
import models.previousRegistrations.SchemeDetailsWithOptionalVatNumber
import models.requests.AuthenticatedDataRequest
import pages.Waypoints
import pages.previousRegistrations.PreviousSchemePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousSchemeSummary {

  def getSummaryLists(
                       previousSchemes: List[SchemeDetailsWithOptionalVatNumber],
                       countryIndex: Index,
                       country: Country,
                       existingSchemes: Seq[PreviousScheme],
                       waypoints: Waypoints
                     )(implicit request: AuthenticatedDataRequest[_], messages: Messages): List[SummaryList] = {


    previousSchemes.zipWithIndex.flatMap { case (scheme, schemeIndex) =>
      request.userAnswers.get(PreviousSchemePage(countryIndex, Index(schemeIndex))).map { previousAnsweredScheme: PreviousScheme =>
        val isExistingScheme = existingSchemes.contains(previousAnsweredScheme)
        SummaryListViewModel(
          rows = Seq(
            PreviousSchemeNumberSummary.row(request.userAnswers, countryIndex, Index(schemeIndex), scheme.previousScheme),
            PreviousIntermediaryNumberSummary.row(request.userAnswers, countryIndex, Index(schemeIndex))
          ).flatten
        ).withCard(
          card = Card(
            title = Some(CardTitle(content = HtmlContent(HtmlFormat.escape(messages(s"previousScheme.$previousAnsweredScheme"))))),
            actions = Some(Actions(
              items = if (!isExistingScheme) {
                Seq(
                  ActionItemViewModel(
                    "site.remove",
                    controllers.previousRegistrations.routes.DeletePreviousSchemeController.onPageLoad(waypoints, countryIndex, Index(schemeIndex)).url
                  ).withVisuallyHiddenText(messages("site.remove.hidden", country.name, HtmlFormat.escape(messages(s"previousScheme.$previousAnsweredScheme"))))
                )
              } else {
                Seq.empty
              }
            ))
          )
        )
      }
    }
  }

}
