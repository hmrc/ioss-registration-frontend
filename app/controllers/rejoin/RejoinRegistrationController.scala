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

package controllers.rejoin

import controllers.actions._
import controllers.rejoin.validation.RejoinRegistrationValidation
import logging.Logging
import models.amend.RegistrationWrapper
import models.domain.PreviousRegistration
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest}
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.rejoin.{CannotRejoinRegistrationPage, RejoinRegistrationPage}
import pages.{CheckAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.rejoin.NewIossReferenceQuery
import services.RegistrationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.summarylist._
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.rejoin.RejoinRegistrationView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RejoinRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationService: RegistrationService,
                                              rejoinRegistrationValidator: RejoinRegistrationValidation,
                                              view: RejoinRegistrationView,
                                              clock: Clock
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with CompletionChecks
    with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndRequireIoss(RejoiningRegistration).async {
    implicit request: AuthenticatedMandatoryIossRequest[AnyContent] =>

      val registrationWrapper: RegistrationWrapper = request.registrationWrapper
      val date = LocalDate.now(clock)
      val canRejoin = registrationWrapper.registration.canRejoinRegistration(date)
      val isCurrentIossAccount: Boolean = true

      if (canRejoin) {
        val thisPage = RejoinRegistrationPage
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, RejoinRegistrationPage.urlFragment))
        defendAgainstInvalidExistingRegistrations(registrationWrapper, waypoints) {

          val iossNumber: String = request.iossNumber
          val userAnswers = request.userAnswers

          val list = detailsList(waypoints, thisPage, userAnswers, isCurrentIossAccount)
          val isValid = validate()(request.request)

          val vatRegistrationDetailsList = SummaryListViewModel(
            rows = Seq(
              VatRegistrationDetailsSummary.rowBusinessName(userAnswers),
              VatRegistrationDetailsSummary.rowIndividualName(userAnswers),
              VatRegistrationDetailsSummary.rowPartOfVatUkGroup(userAnswers),
              VatRegistrationDetailsSummary.rowUkVatRegistrationDate(userAnswers),
              VatRegistrationDetailsSummary.rowBusinessAddress(userAnswers)
            ).flatten
          )

          Future.successful(Ok(view(waypoints, vatRegistrationDetailsList, list, iossNumber, isValid)))
        }
      }
      else {
        Future.successful(Redirect(CannotRejoinRegistrationPage.route(EmptyWaypoints).url))
      }
  }

  private def defendAgainstInvalidExistingRegistrations(registrationWrapper: RegistrationWrapper, waypoints: Waypoints)
                                                       (successCall: => Future[Result])(implicit hc: HeaderCarrier,
                                                                                        request: AuthenticatedMandatoryIossRequest[AnyContent],
                                                                                        ec: ExecutionContext): Future[Result] = {
    //Causes a bun fight if done outside this scope due to implicits fight
    implicit val authenticatedDataRequest: AuthenticatedDataRequest[AnyContent] = request.request

    rejoinRegistrationValidator.validateEuRegistrations(registrationWrapper, waypoints).flatMap {
      case Left(validationFailureRedirectLocation) => Future.successful(Redirect(validationFailureRedirectLocation))
      case _ => successCall
    }
  }

  private def detailsList(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers, isCurrentIossAccount: Boolean)
                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]) = {
    SummaryListViewModel(
      rows =
        (getTradingNameRows(waypoints, sourcePage, userAnswers, isCurrentIossAccount) ++
          getPreviouslyRegisteredRows(waypoints, sourcePage, userAnswers, isCurrentIossAccount) ++
          getRegisteredInEuRows(waypoints, sourcePage, userAnswers, isCurrentIossAccount) ++
          getWebsitesRows(waypoints, sourcePage, userAnswers, isCurrentIossAccount) ++
          getBusinessContactDetailsRows(waypoints, sourcePage, userAnswers) ++
          getBankDetailsRows(waypoints, sourcePage, userAnswers)
          ).flatten
    )
  }

  private def getTradingNameRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers, isCurrentIossAccount: Boolean)
                                (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]): Seq[Option[SummaryListRow]] = {
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(userAnswers, waypoints, sourcePage, isCurrentIossAccount)
    Seq(HasTradingNameSummary.row(userAnswers, waypoints, sourcePage, isCurrentIossAccount).map { sr =>
      if (tradingNameSummaryRow.isDefined) {
        sr.withCssClass("govuk-summary-list__row--no-border")
      } else {
        sr
      }
    },
      tradingNameSummaryRow)
  }

  private def getPreviouslyRegisteredRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers, isCurrentIossAccount: Boolean)
                                         (implicit request: AuthenticatedMandatoryIossRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(
      answers = userAnswers,
      existingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(request.previousEURegistrationDetails),
      waypoints = waypoints,
      sourcePage = sourcePage,
      isCurrentIossAccount
    )

    val lockEditing: Boolean = userAnswers.get(PreviouslyRegisteredPage).contains(true)

    Seq(
      PreviouslyRegisteredSummary.row(userAnswers, waypoints, sourcePage, lockEditing, isCurrentIossAccount).map { sr =>
        if (previousRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousRegistrationSummaryRow
    )
  }

  private def getRegisteredInEuRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers, isCurrentIossAccount: Boolean)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(userAnswers, waypoints, sourcePage, isCurrentIossAccount)
    Seq(
      TaxRegisteredInEuSummary.row(userAnswers, waypoints, sourcePage, isCurrentIossAccount).map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow
    )
  }

  private def getWebsitesRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers, isCurrentIossAccount: Boolean)
                             (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(WebsiteSummary.checkAnswersRow(userAnswers, waypoints, sourcePage, isCurrentIossAccount))
  }

  private def getBusinessContactDetailsRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers)
                                           (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(
      BusinessContactDetailsSummary.rowContactName(userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BusinessContactDetailsSummary.rowTelephoneNumber(userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BusinessContactDetailsSummary.rowEmailAddress(userAnswers, waypoints, sourcePage)
    )
  }

  private def getBankDetailsRows(waypoints: Waypoints, sourcePage: CheckAnswersPage, userAnswers: UserAnswers)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {
    Seq(
      BankDetailsSummary.rowAccountName(userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BankDetailsSummary.rowBIC(userAnswers, waypoints, sourcePage).map(_.withCssClass("govuk-summary-list__row--no-border")),
      BankDetailsSummary.rowIBAN(userAnswers, waypoints, sourcePage)
    )
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.authAndRequireIoss(RejoiningRegistration).async {
    implicit request =>

      val canRejoin = request.registrationWrapper.registration.canRejoinRegistration(LocalDate.now(clock))

      if (canRejoin) {

        defendAgainstInvalidExistingRegistrations(request.registrationWrapper, waypoints) {

          getFirstValidationErrorRedirect(waypoints)(request.request) match {
            case Some(errorRedirect) => if (incompletePrompt) {
              errorRedirect.toFuture
            } else {
              Redirect(routes.RejoinRegistrationController.onPageLoad()).toFuture
            }

            case None =>
              val userAnswers = request.userAnswers
              registrationService.amendRegistration(
                answers = userAnswers,
                registration = request.registrationWrapper.registration,
                vrn = request.vrn,
                iossNumber = request.iossNumber,
                rejoin = true
              ).flatMap {
                case Right(amendRegistrationResponse) =>
                  userAnswers.set(NewIossReferenceQuery, amendRegistrationResponse.iossReference) match {
                    case Failure(throwable) =>
                      logger.error(s"Unexpected result on updating answers with new IOSS Reference: ${throwable.getMessage}", throwable)
                      Future.successful(Redirect(routes.ErrorSubmittingRejoinController.onPageLoad()))
                    case Success(updatedUserAnswers) =>
                      cc.sessionRepository.set(updatedUserAnswers).map { _ =>
                        Redirect(RejoinRegistrationPage.navigate(EmptyWaypoints, userAnswers, userAnswers).route)
                      }
                  }
                case Left(e) =>
                  logger.error(s"Unexpected result on submit: ${e.body}")
                  Future.successful(Redirect(routes.ErrorSubmittingRejoinController.onPageLoad()))
              }
          }
        }
      } else {
        Future.successful(Redirect(CannotRejoinRegistrationPage.route(EmptyWaypoints).url))
      }
  }
}
