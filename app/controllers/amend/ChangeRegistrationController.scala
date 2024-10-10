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
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.{CheckAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PreviousRegistrationIossNumberQuery
import services.{AccountService, RegistrationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.summarylist._
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.amend.ChangeRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationService: RegistrationService,
                                              accountService: AccountService,
                                              view: ChangeRegistrationView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(isPreviousRegistration: Boolean): Action[AnyContent] = {
    val modifyingExistingRegistrationMode = if (isPreviousRegistration) {
      AmendingPreviousRegistration
    } else {
      AmendingActiveRegistration
    }

    cc.authAndRequireIoss(modifyingExistingRegistrationMode, restrictFromPreviousRegistrations = false, waypoints = EmptyWaypoints).async {
      implicit request: AuthenticatedMandatoryIossRequest[AnyContent] =>

        val futurePreviousRegistrations = if(request.hasMultipleIossEnrolments) {
          accountService.getPreviousRegistrations()
        } else {
          Seq.empty.toFuture
        }

        futurePreviousRegistrations.map { previousRegistrations =>

          val selectedPreviousRegistration: Option[String] = request.userAnswers.get(PreviousRegistrationIossNumberQuery)

          val thisPage =
            if (isPreviousRegistration) {
              ChangePreviousRegistrationPage
            } else {
              ChangeRegistrationPage
            }

          val waypoints =
            if (isPreviousRegistration) {
              EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))
            } else {
              EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))
            }

          val iossNumber: String = selectedPreviousRegistration.getOrElse(request.iossNumber)

          val vatRegistrationDetailsList = SummaryListViewModel(
            rows = Seq(
              VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
              VatRegistrationDetailsSummary.rowIndividualName(request.userAnswers),
              VatRegistrationDetailsSummary.rowPartOfVatUkGroup(request.userAnswers),
              VatRegistrationDetailsSummary.rowUkVatRegistrationDate(request.userAnswers),
              VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
            ).flatten
          )

          val isValid = validate()(request.request)
          val hasPreviousRegistrations: Boolean = previousRegistrations.nonEmpty
          val isCurrentIossAccount: Boolean = request.iossNumber == iossNumber
          val list = detailsList(waypoints, thisPage, isCurrentIossAccount)

          Ok(view(waypoints, vatRegistrationDetailsList, list, iossNumber, isValid, hasPreviousRegistrations, isCurrentIossAccount))
        }
    }
  }


  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] =
    cc.authAndRequireIoss(AmendingActiveRegistration, restrictFromPreviousRegistrations = false, waypoints = waypoints).async {
      implicit request =>

        val iossNumber: String = request.userAnswers.get(PreviousRegistrationIossNumberQuery).getOrElse(request.iossNumber)

        getFirstValidationErrorRedirect(waypoints)(request.request) match {
          case Some(errorRedirect) => if (incompletePrompt) {
            errorRedirect.toFuture
          } else {
            Redirect(routes.ChangeRegistrationController.onPageLoad(waypoints.inPreviousRegistrationAmend)).toFuture
          }

          case None =>
            registrationService.amendRegistration(
              answers = request.userAnswers,
              registration = request.registrationWrapper.registration,
              vrn = request.vrn,
              iossNumber,
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

  private def detailsList(waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]) = {
    SummaryListViewModel(
      rows =
        (getTradingNameRows(waypoints, sourcePage, isCurrentIossAccount) ++
          getPreviouslyRegisteredRows(waypoints, sourcePage, isCurrentIossAccount) ++
          getRegisteredInEuRows(waypoints, sourcePage, isCurrentIossAccount) ++
          getWebsitesRows(waypoints, sourcePage, isCurrentIossAccount) ++
          getBusinessContactDetailsRows(waypoints, sourcePage) ++
          getBankDetailsRows(waypoints, sourcePage)
          ).flatten
    )
  }

  private def getTradingNameRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage, isCurrentIossAccount)
    Seq(HasTradingNameSummary.row(request.userAnswers, waypoints, sourcePage, isCurrentIossAccount).map { sr =>
      if (tradingNameSummaryRow.isDefined) {
        sr.withCssClass("govuk-summary-list__row--no-border")
      } else {
        sr
      }
    },
      tradingNameSummaryRow)
  }

  private def getPreviouslyRegisteredRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(
      answers = request.userAnswers,
      existingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(request.previousEURegistrationDetails),
      waypoints = waypoints,
      sourcePage = sourcePage,
      isCurrentIossAccount
    )

    val lockEditing: Boolean = request.userAnswers.get(PreviouslyRegisteredPage).contains(true)

    Seq(
      PreviouslyRegisteredSummary.row(request.userAnswers, waypoints, sourcePage, lockEditing, isCurrentIossAccount).map { sr =>
        if (previousRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousRegistrationSummaryRow
    )
  }

  private def getRegisteredInEuRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage, isCurrentIossAccount)
    Seq(
      TaxRegisteredInEuSummary.row(request.userAnswers, waypoints, sourcePage, isCurrentIossAccount).map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow
    )
  }

  private def getWebsitesRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isCurrentIossAccount: Boolean)
                             (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(WebsiteSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage, isCurrentIossAccount))
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
