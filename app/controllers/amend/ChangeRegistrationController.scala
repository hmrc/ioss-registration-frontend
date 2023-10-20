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

package controllers.amend

import controllers.actions._
import models.CheckMode
import pages.amend.AmendYourAnswersPage
import pages.{EmptyWaypoints, Waypoint}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.summarylist._
import views.html.amend.ChangeRegistrationView

import javax.inject.Inject

class ChangeRegistrationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: ChangeRegistrationView
                                     ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetData(inAmend = true) {
    implicit request =>

      val thisPage = AmendYourAnswersPage

      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, AmendYourAnswersPage.urlFragment))

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

      val websiteSummaryRow = WebsiteSummary.checkAnswersRow(request.userAnswers, waypoints, thisPage)
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(request.userAnswers, waypoints, thisPage)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(request.userAnswers, Seq.empty, waypoints, thisPage)
      val maybeTaxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.row(request.userAnswers, waypoints, thisPage)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(request.userAnswers, waypoints, thisPage)
      val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(request.userAnswers, waypoints, thisPage)
      val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(request.userAnswers, waypoints, thisPage)
      val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(request.userAnswers, waypoints, thisPage)
      val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(request.userAnswers, waypoints, thisPage)
      val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(request.userAnswers, waypoints, thisPage)
      val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(request.userAnswers, waypoints, thisPage)


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
          previouslyRegisteredSummaryRow.map { sr =>
            if (previousRegistrationSummaryRow.isDefined) {
              sr.withCssClass("govuk-summary-list__row--no-border")
            } else {
              sr
            }
          },
          previousRegistrationSummaryRow,
          maybeTaxRegisteredInEuSummaryRow.map { taxRegisteredInEuSummaryRow =>
            if (euDetailsSummaryRow.nonEmpty) {
              taxRegisteredInEuSummaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              taxRegisteredInEuSummaryRow
            }
          },
          euDetailsSummaryRow,
          websiteSummaryRow,
          businessContactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          businessContactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          businessContactDetailsEmailSummaryRow,
          bankDetailsAccountNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          bankDetailsBicSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          bankDetailsIbanSummaryRow
        ).flatten
      )
      Ok(view(waypoints, vatRegistrationDetailsList, list))
  }

  def onSubmit(): Action[AnyContent] = Action {
    // TODO
    println("Amend submit")
    Ok
  }
}