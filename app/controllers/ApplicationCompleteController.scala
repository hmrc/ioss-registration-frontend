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

import config.FrontendAppConfig
import controllers.actions.*
import formats.Format.{dateFormatter, dateMonthYearFormatter}
import models.{TradingName, UserAnswers}
import models.ossRegistration.OssRegistration
import models.requests.AuthenticatedDataRequest
import pages.{BankDetailsPage, BusinessContactDetailsPage, EmptyWaypoints, JourneyRecoveryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.etmp.EtmpEnrolmentResponseQuery
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ApplicationCompleteView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.util.{Failure, Success}


class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               view: ApplicationCompleteView,
                                               frontendAppConfig: FrontendAppConfig,
                                               clock: Clock
                                             ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] =  (cc.actionBuilder andThen cc.identify andThen cc.getData andThen cc.requireData(isInAmendMode = false)) {
    implicit request =>

      (for {
        etmpEnrolmentResponse <- request.userAnswers.get(EtmpEnrolmentResponseQuery)
        organisationName <- getOrganisationName(request.userAnswers)
      } yield {

        val iossReferenceNumber = etmpEnrolmentResponse.iossReference
        val ossRegistration = request.latestOssRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations

        val commencementDate = LocalDate.now(clock)
        val returnStartDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
        val includedSalesDate = commencementDate.withDayOfMonth(1)
        val list: SummaryList = detailsList(ossRegistration)

        Ok(view(
          iossReferenceNumber,
          organisationName,
          includedSalesDate.format(dateMonthYearFormatter),
          returnStartDate.format(dateFormatter),
          includedSalesDate.format(dateFormatter),
          frontendAppConfig.feedbackUrl,
          ossRegistration,
          numberOfIossRegistrations,
          list
        ))
      }).getOrElse(Redirect(JourneyRecoveryPage.route(EmptyWaypoints)))
  }

  private def getOrganisationName(answers: UserAnswers): Option[String] =
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => None
    }

  private def detailsList(ossRegistration: Option[OssRegistration])(implicit request: AuthenticatedDataRequest[AnyContent]) = {
    ossRegistration match {
      case Some(registration) =>
        SummaryListViewModel(
          rows = (
            getHasTradingNameRows(registration) ++
              getTradingNameRows(registration) ++
              getBusinessContactDetailsRows(registration) ++
              getBankDetailsRows(registration)
            ).flatten
        )
      case None =>
        SummaryListViewModel(rows = Seq.empty)
    }
  }

  private def getHasTradingNameRows(ossRegistration: OssRegistration)
                                   (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = ossRegistration.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNames).getOrElse(List.empty)
    val hasChangedToNo = amendedAnswers.isEmpty && originalAnswers.nonEmpty
    val hasChangedToYes = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || originalAnswers.isEmpty
    val notAmended = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || amendedAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo || hasChangedToYes) {
      Seq(HasTradingNameSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(ossRegistration: OssRegistration)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = ossRegistration.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNames).map(_.map(_.name)).getOrElse(List.empty)
    val addedTradingName = amendedAnswers.diff(originalAnswers)
    val removedTradingNames = originalAnswers.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => TradingName(amended)
    } ++ amendedAnswers.drop(originalAnswers.size).map(tradingName => TradingName(tradingName))

    val addedTradingNameRow = if (addedTradingName.nonEmpty) {
      request.userAnswers.set(AllTradingNames, changedTradingName) match {
        case Success(amendedUserAnswer: UserAnswers) =>
          Some(TradingNameSummary.amendedAnswersRow(amendedUserAnswer))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedTradingNameRow = Some(TradingNameSummary.removedAnswersRow(removedTradingNames))

    Seq(addedTradingNameRow, removedTradingNameRow).flatten
  }

  private def getBusinessContactDetailsRows(ossRegistration: OssRegistration)
                                           (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalContactName = ossRegistration.contactDetails.fullName
    val originalTelephone = ossRegistration.contactDetails.telephoneNumber
    val originalEmail = ossRegistration.contactDetails.emailAddress
    val amendedUA = request.userAnswers.get(BusinessContactDetailsPage)

    Seq(
      if (!amendedUA.map(_.fullName).contains(originalContactName)) {
        BusinessContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.telephoneNumber).contains(originalTelephone)) {
        BusinessContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.emailAddress).contains(originalEmail)) {
        BusinessContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getBankDetailsRows(ossRegistration: OssRegistration)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = ossRegistration.bankDetails
    val amendedUA = request.userAnswers.get(BankDetailsPage)

    Seq(
      if (!amendedUA.map(_.accountName).contains(originalAnswers.accountName)) {
        BankDetailsSummary.amendedRowAccountName(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.bic).contains(originalAnswers.bic)) {
        BankDetailsSummary.amendedRowBIC(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.iban).contains(originalAnswers.iban)) {
        BankDetailsSummary.amendedRowIBAN(request.userAnswers)
      } else {
        None
      }
    )
  }
}
