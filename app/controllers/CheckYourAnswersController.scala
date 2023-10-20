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

import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.responses.ConflictFound
import pages.filters.CannotRegisterAlreadyRegisteredPage
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoints}
import logging.Logging
import models.CheckMode
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import pages.{CheckYourAnswersPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.etmp.EtmpEnrolmentResponseQuery
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.CompletionChecks
import utils.FutureSyntax._
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.summarylist._
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject
import scala.concurrent.Future

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            registrationService: RegistrationService,
                                            view: CheckYourAnswersView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndGetDataAndCheckVerifyEmail() {
    implicit request =>

      val thisPage = CheckYourAnswersPage

      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))

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
      val isValid = validate()
      Ok(view(waypoints, vatRegistrationDetailsList, list, isValid))
  }

  // TODO - > Need to create Audit Service and pass request.userAnswers (as JSON if not already)
  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      registrationService.createRegistrationRequest(request.userAnswers, request.vrn).flatMap {
        case Right(response) =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EtmpEnrolmentResponseQuery, response))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)

        case Left(ConflictFound) =>
          logger.warn("Conflict found on registration creation submission")
          Redirect(CannotRegisterAlreadyRegisteredPage.route(waypoints)).toFuture

        case Left(error) =>
          // TODO Add SaveAndContinue when created
          logger.error(s"Internal server error on registration creation submission: ${error.body}")
          // TODO ErrorSubmittingRegistrationPage
          // Remove InternalServerError
          InternalServerError(Json.toJson(s"An internal server error occurred with error: ${error.body}")).toFuture
      }

  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getFirstValidationErrorRedirect(waypoints) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(routes.CheckYourAnswersController.onPageLoad()).toFuture
        }
        case None => Future.successful(Ok)
      }
  }
}
