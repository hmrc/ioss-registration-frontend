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

package controllers

import com.google.inject.Inject
import controllers.actions.AuthenticatedControllerComponents
import pages.{CheckYourAnswersPage, EmptyWaypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.VatRegistrationDetailsSummary
import viewmodels.checkAnswers.{BusinessContactDetailsSummary, HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(request.userAnswers, EmptyWaypoints)
      val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(request.userAnswers, EmptyWaypoints)
      val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(request.userAnswers, EmptyWaypoints)

      val thisPage = CheckYourAnswersPage
      val waypoints = EmptyWaypoints

      val vatRegistrationDetailsList = SummaryListViewModel(
        rows = Seq(
          VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
          VatRegistrationDetailsSummary.rowIndividualName(request.userAnswers),
          VatRegistrationDetailsSummary.rowPartOfVatUkGroup(request.userAnswers),
          VatRegistrationDetailsSummary.rowUkVatRegistrationDate(request.userAnswers),
          VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
        ).flatten
      )

      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(request.userAnswers, waypoints, thisPage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
            if (tradingNameSummaryRow.nonEmpty) {
              hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              hasTradingNameSummaryRow
            }
          },
          tradingNameSummaryRow,
          businessContactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          businessContactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          businessContactDetailsEmailSummaryRow
        ).flatten
      )

      Ok(view(vatRegistrationDetailsList, list))
  }
}
