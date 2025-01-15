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

import models.{Country, Index, UserAnswers}
import models.domain.PreviousRegistration
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import pages.previousRegistrations._
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.previousRegistration._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import utils.CheckExistingRegistrations
import viewmodels.ListItemWrapper
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousRegistrationSummary {

  def row(answers: UserAnswers,
          existingPreviousRegistrations: Seq[PreviousRegistration],
          waypoints: Waypoints,
          sourcePage: AddItemPage): Seq[ListItemWrapper] =
    answers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).getOrElse(List.empty).zipWithIndex.map {
      case (details, index) =>

        ListItemWrapper(
          ListItem(
            name = HtmlFormat.escape(details.previousEuCountry.name).toString,
            changeUrl = CheckPreviousSchemeAnswersPage(Index(index)).changeLink(waypoints, sourcePage).url,
            removeUrl = DeletePreviousRegistrationPage(Index(index)).route(waypoints).url
          ),
          !CheckExistingRegistrations.existingPreviousRegistration(details.previousEuCountry, existingPreviousRegistrations)
        )
    }

  def checkAnswersRow(
                       answers: UserAnswers,
                       existingPreviousRegistrations: Seq[PreviousRegistration],
                       waypoints: Waypoints,
                       sourcePage: CheckAnswersPage,
                       isCurrentIossAccount: Boolean
                     )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllPreviousRegistrationsQuery).map {
      previousRegistrations =>
        val value = previousRegistrations.map {
          details =>
            HtmlFormat.escape(details.previousEuCountry.name)
        }.mkString("<br/>")


        val currentAnswerCountries = previousRegistrations.map(_.previousEuCountry)
        val existingCountries = existingPreviousRegistrations.map(previousRegistration => previousRegistration.country)
        val sameListOfCountries: Boolean = currentAnswerCountries.sortBy(_.code) == existingCountries.sortBy(_.code)

        val listRowViewModel = SummaryListRowViewModel(
          key = "previousRegistrations.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = if (isCurrentIossAccount) {
            Seq(
              if (sameListOfCountries) {
                ActionItemViewModel("site.add", controllers.previousRegistrations.routes.AddPreviousRegistrationController.onPageLoad(waypoints).url)
                  .withVisuallyHiddenText(messages("previousRegistrations.add.hidden"))
              } else {
                ActionItemViewModel("site.change", AddPreviousRegistrationPage().changeLink(waypoints, sourcePage).url)
                  .withVisuallyHiddenText(messages("previousRegistrations.change.hidden"))
              }
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
    answers.get(AllPreviousRegistrationsQuery).map {
      previousRegistrations =>
        val value = previousRegistrations.map {
          details =>
            HtmlFormat.escape(details.previousEuCountry.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = KeyViewModel("previousRegistrations.checkYourAnswersLabel").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value)),
        )
    }

  def changedAnswersRow(changedAnswers: Seq[Country])(implicit messages: Messages): Option[SummaryListRow] =

    if (changedAnswers.nonEmpty) {
      val value = changedAnswers.map {
        details =>
          HtmlFormat.escape(details.name)
      }.mkString("<br/>")

      Some(
        SummaryListRowViewModel(
          key = KeyViewModel("previousRegistrations.checkYourAnswersLabel.changed").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value)),
        )
      )
    } else {
      None
    }

}
