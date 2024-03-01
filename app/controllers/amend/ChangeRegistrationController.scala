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
import logging.Logging
import models.CheckMode
import models.domain.PreviousRegistration
import models.requests.AuthenticatedMandatoryIossRequest
import pages.{CheckAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviouslyRegisteredSummary, PreviousRegistrationSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.summarylist._
import views.html.amend.ChangeRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationService: RegistrationService,
                                              view: ChangeRegistrationView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndRequireIoss(AmendingActiveRegistration) {
    implicit request: AuthenticatedMandatoryIossRequest[AnyContent] =>

  // TODO -> pass through boolean to view for previousReg exist

      val thisPage = ChangeRegistrationPage

      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))

      val iossNumber: String = request.iossNumber

      val vatRegistrationDetailsList = SummaryListViewModel(
        rows = Seq(
          VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
          VatRegistrationDetailsSummary.rowIndividualName(request.userAnswers),
          VatRegistrationDetailsSummary.rowPartOfVatUkGroup(request.userAnswers),
          VatRegistrationDetailsSummary.rowUkVatRegistrationDate(request.userAnswers),
          VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
        ).flatten
      )

      val list = detailsList(waypoints, thisPage)
      val isValid = validate()(request.request)
      Ok(view(waypoints, vatRegistrationDetailsList, list, iossNumber, isValid))
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.authAndRequireIoss(AmendingActiveRegistration).async {
    implicit request =>
      getFirstValidationErrorRedirect(waypoints)(request.request) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(routes.ChangeRegistrationController.onPageLoad()).toFuture
        }

        case None =>
          registrationService.amendRegistration(
            answers = request.userAnswers,
            registration = request.registrationWrapper.registration,
            vrn = request.vrn,
            request.iossNumber,
            rejoin = false
          ).map {
            case Right(_) =>
              Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
            case Left(e) =>
              logger.error(s"Unexpected result on submit: ${e.body}")
              Redirect(routes.ErrorSubmittingAmendmentController.onPageLoad())
          }
      }
  }

  private def detailsList(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]) = {
    SummaryListViewModel(
      rows =
        (getTradingNameRows(waypoints, sourcePage) ++
          getPreviouslyRegisteredRows(waypoints, sourcePage) ++
          getRegisteredInEuRows(waypoints, sourcePage) ++
          getWebsitesRows(waypoints, sourcePage) ++
          getBusinessContactDetailsRows(waypoints, sourcePage) ++
          getBankDetailsRows(waypoints, sourcePage)
          ).flatten
    )
  }

  private def getTradingNameRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage)
    Seq(HasTradingNameSummary.row(request.userAnswers, waypoints, sourcePage).map { sr =>
      if (tradingNameSummaryRow.isDefined) {
        sr.withCssClass("govuk-summary-list__row--no-border")
      } else {
        sr
      }
    },
      tradingNameSummaryRow)
  }

  private def getPreviouslyRegisteredRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(
      answers = request.userAnswers,
      existingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(request.previousEURegistrationDetails),
      waypoints = waypoints, sourcePage =
        sourcePage
    )

    val lockEditing: Boolean = request.userAnswers.get(PreviouslyRegisteredPage).contains(true)

    Seq(
      PreviouslyRegisteredSummary.row(request.userAnswers, waypoints, sourcePage, lockEditing).map { sr =>
        if (previousRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousRegistrationSummaryRow
    )
  }

  private def getRegisteredInEuRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage)
    Seq(
      TaxRegisteredInEuSummary.row(request.userAnswers, waypoints, sourcePage).map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow
    )
  }

  private def getWebsitesRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                             (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(WebsiteSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage))
  }

  private def getBusinessContactDetailsRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                                           (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(
      BusinessContactDetailsSummary.rowContactName(request.userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BusinessContactDetailsSummary.rowTelephoneNumber(request.userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BusinessContactDetailsSummary.rowEmailAddress(request.userAnswers, waypoints, sourcePage)
    )
  }

  private def getBankDetailsRows(waypoints: Waypoints, sourcePage: CheckAnswersPage)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(
      BankDetailsSummary.rowAccountName(request.userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BankDetailsSummary.rowBIC(request.userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BankDetailsSummary.rowIBAN(request.userAnswers, waypoints, sourcePage)
    )
  }
}
