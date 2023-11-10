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

import base.SpecBase
import controllers.actions.{FakeIossRequiredAction, IossRequiredAction}
import controllers.amend.{routes => amendRoutes}
import models.amend.RegistrationWrapper
import models.{CheckMode, UserAnswers}
import pages.amend.ChangeRegistrationPage
import pages.{EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.SummaryListFluency
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.amend.ChangeRegistrationView

class ChangeRegistrationControllerSpec extends SpecBase with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val amendYourAnswersPage = ChangeRegistrationPage

  "ChangeRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val registrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapper)))
        .build()

      running(application) {

        val request = FakeRequest(GET, amendRoutes.ChangeRegistrationController.onPageLoad().url)

        implicit val msgs: Messages = messages(application)
        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeRegistrationView]

        val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))
        val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, vatInfoList, list, iossNumber)(request, messages(application)).toString
      }
    }
  }

  private def getChangeRegistrationVatRegistrationDetailsSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {
    Seq(
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowPartOfVatUkGroup(answers),
      VatRegistrationDetailsSummary.rowUkVatRegistrationDate(answers),
      VatRegistrationDetailsSummary.rowBusinessAddress(answers)
    ).flatten
  }

  private def getChangeRegistrationSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.row(answers, waypoints, amendYourAnswersPage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(answers, waypoints, amendYourAnswersPage)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, amendYourAnswersPage)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, Seq.empty, waypoints, amendYourAnswersPage)
    val taxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.row(answers, waypoints, amendYourAnswersPage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(answers, waypoints, amendYourAnswersPage)
    val websiteSummaryRow = WebsiteSummary.checkAnswersRow(answers, waypoints, amendYourAnswersPage)
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(answers, waypoints, amendYourAnswersPage)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(answers, waypoints, amendYourAnswersPage)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(answers, waypoints, amendYourAnswersPage)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(answers, waypoints, amendYourAnswersPage)
    val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(answers, waypoints, amendYourAnswersPage)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(answers, waypoints, amendYourAnswersPage)

    Seq(
      hasTradingNameSummaryRow.map { sr =>
        if (tradingNameSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
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
      taxRegisteredInEuSummaryRow.map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
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
  }
}
