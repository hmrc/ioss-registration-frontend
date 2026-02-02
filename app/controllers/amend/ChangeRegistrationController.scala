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

import controllers.actions.*
import logging.Logging
import models.CheckMode
import models.audit.AmendRegistrationAuditModel
import models.audit.RegistrationAuditType.AmendRegistration
import models.audit.SubmissionResult.{Failure, Success}
import models.domain.PreviousRegistration
import models.etmp.{EtmpExclusion, EtmpExclusionReason}
import models.requests.AuthenticatedMandatoryIossRequest
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.{CheckAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PreviousRegistrationIossNumberQuery
import services.{AccountService, AuditService, RegistrationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.summarylist.*
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.amend.ChangeRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationService: RegistrationService,
                                              accountService: AccountService,
                                              auditService: AuditService,
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

          val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption.flatMap { exclusion =>
            exclusion.exclusionReason match {
              case EtmpExclusionReason.Reversal => None
              case _ => Some(exclusion)
            }
          }
          val isExcluded = maybeExclusion.isDefined

          val thisPage =
            if (isPreviousRegistration) {
              ChangePreviousRegistrationPage
            } else {
              ChangeRegistrationPage
            }

          val waypoints =
            if (isPreviousRegistration) {
              EmptyWaypoints.setNextWaypoint(Waypoint(ChangePreviousRegistrationPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))
            } else {
              EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
            }

          val iossNumber: String = selectedPreviousRegistration.getOrElse(request.iossNumber)

          val vatRegistrationDetailsList = SummaryListViewModel(
            rows = Seq(
              VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
              VatRegistrationDetailsSummary.rowIndividualName(request.userAnswers),
              VatRegistrationDetailsSummary.rowUkVatRegistrationDate(request.userAnswers),
              VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
            ).flatten
          )

          val isValid = validate()(request.request)
          val hasPreviousRegistrations: Boolean = previousRegistrations.nonEmpty
          val isCurrentIossAccount: Boolean = request.iossNumber == iossNumber
          val list = detailsList(waypoints, thisPage, isExcluded)

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
              case Right(response) =>
                auditService.audit(
                  AmendRegistrationAuditModel.build(
                    registrationAuditType = AmendRegistration,
                    userAnswers = request.userAnswers,
                    amendRegistrationResponse = Some(response),
                    submissionResult = Success
                  )(request.request)
                )
                Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
              case Left(e) =>
                logger.error(s"Unexpected result on submit: ${e.body}")
                auditService.audit(
                  AmendRegistrationAuditModel.build(
                    registrationAuditType = AmendRegistration,
                    userAnswers = request.userAnswers,
                    amendRegistrationResponse = None,
                    submissionResult = Failure
                  )(request.request)
                )
                Redirect(routes.ErrorSubmittingAmendmentController.onPageLoad())
            }
        }
    }

  private def detailsList(waypoints: Waypoints, sourcePage: CheckAnswersPage, isExcluded: Boolean)
                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]) = {
    SummaryListViewModel(
      rows =
        (getTradingNameRows(waypoints, sourcePage, isExcluded) ++
          getPreviouslyRegisteredRows(waypoints, sourcePage, isExcluded) ++
          getRegisteredInEuRows(waypoints, sourcePage, isExcluded) ++
          getWebsitesRows(waypoints, sourcePage, isExcluded) ++
          getBusinessContactDetailsRows(waypoints, sourcePage) ++
          getBankDetailsRows(waypoints, sourcePage)
          ).flatten
    )
  }

  private def getTradingNameRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isExcluded: Boolean)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    if (isExcluded) {
      val tradingNameRow = TradingNameSummary.checkAnswersRowWithoutAction(request.userAnswers, waypoints)
      Seq(
        HasTradingNameSummary.rowWithoutAction(request.userAnswers, waypoints).map { sr =>
          if (tradingNameRow.isDefined) sr.withCssClass("govuk-summary-list__row--no-border") else sr
        },
        tradingNameRow
      )
    } else {
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage)
      Seq(
        HasTradingNameSummary.row(request.userAnswers, waypoints, sourcePage).map { sr =>
          if (tradingNameSummaryRow.isDefined) sr.withCssClass("govuk-summary-list__row--no-border") else sr
        },
        tradingNameSummaryRow
      )
    }
  }

  private def getPreviouslyRegisteredRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isExcluded: Boolean)
                                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val previousRegistrationSummaryRow = if (isExcluded) {
      PreviousRegistrationSummary.checkAnswersRowWithoutAction(
        answers = request.userAnswers,
        existingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(request.previousEURegistrationDetails),
        waypoints = waypoints
      )
    } else {
      PreviousRegistrationSummary.checkAnswersRow(
        answers = request.userAnswers,
        existingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(request.previousEURegistrationDetails),
        waypoints = waypoints,
        sourcePage = sourcePage
      )
    }

    val lockEditing: Boolean = request.userAnswers.get(PreviouslyRegisteredPage).contains(true)

    val previouslyRegisteredRow = if (isExcluded) {
      PreviouslyRegisteredSummary.rowWithoutAction(request.userAnswers, waypoints, lockEditing)
    } else {
      PreviouslyRegisteredSummary.row(request.userAnswers, waypoints, sourcePage, lockEditing)
    }

    Seq(
      previouslyRegisteredRow.map { sr =>
        if (previousRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousRegistrationSummaryRow
    )
  }

  private def getRegisteredInEuRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isExcluded: Boolean)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val euDetailsSummaryRow = if (isExcluded) {
      EuDetailsSummary.checkAnswersRowWithoutAction(request.userAnswers, waypoints)
    } else {
      EuDetailsSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage)
    }

    val taxRegisteredInEuRow = if (isExcluded) {
      TaxRegisteredInEuSummary.rowWithoutAction(request.userAnswers, waypoints)
    } else {
      TaxRegisteredInEuSummary.row(request.userAnswers, waypoints, sourcePage)
    }

    Seq(
      taxRegisteredInEuRow.map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow
    )
  }

  private def getWebsitesRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, isExcluded: Boolean)
                             (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    if (isExcluded) {
      Seq(WebsiteSummary.checkAnswersRowWithoutAction(request.userAnswers, waypoints))
    } else {
      Seq(WebsiteSummary.checkAnswersRow(request.userAnswers, waypoints, sourcePage))
    }
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