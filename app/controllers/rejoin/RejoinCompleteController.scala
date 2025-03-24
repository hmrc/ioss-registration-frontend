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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.*
import models.amend.RegistrationWrapper
import models.{TradingName, UserAnswers}
import models.requests.AuthenticatedDataRequest
import pages.{BankDetailsPage, BusinessContactDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.rejoin.NewIossReferenceQuery
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinCompleteView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RejoinCompleteController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          view: RejoinCompleteView,
                                          frontendAppConfig: FrontendAppConfig,
                                          registrationConnector: RegistrationConnector,
                                          clock: Clock
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = (cc.actionBuilder andThen cc.identify andThen cc.getData andThen cc.requireData(isInAmendMode = true)).async {
    implicit request => {
      registrationConnector.getSavedExternalEntry().map { externalEntryUrl =>
        val newIossReference = getNewIossReference(request.userAnswers)
        val organisationName = getOrganisationName(request.userAnswers)
        val savedUrl = externalEntryUrl.fold(_ => None, _.url)

        val commencementDate = LocalDate.now(clock)
        val returnStartDate: LocalDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
        val includedSalesDate = commencementDate.withDayOfMonth(1)
        val ossRegistration = request.latestOssRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations
        val originalRegistration = request.registrationWrapper
        val list: SummaryList = detailsList(originalRegistration)
        
        Ok(
          view(
            vrn = request.vrn,
            feedbackLink = frontendAppConfig.feedbackUrl,
            externalUrl = savedUrl,
            yourAccountUrl = frontendAppConfig.iossYourAccountUrl,
            organisationName = organisationName,
            newIossReference = newIossReference,
            commencementDate = commencementDate,
            returnStartDate = returnStartDate,
            includedSalesDate = includedSalesDate,
            ossRegistration = ossRegistration,
            numberOfIossRegistrations = numberOfIossRegistrations,
            list = list
          )
        )
      }
    }
  }

  private def getNewIossReference(answers: UserAnswers) = {
    answers.get(NewIossReferenceQuery).getOrElse(throw new RuntimeException("NewIossReference has not been set in answers"))
  }

  private def getOrganisationName(answers: UserAnswers): String = {
    answers.vatInfo.flatMap { vatInfo =>
      vatInfo.organisationName.fold(vatInfo.individualName)(Some.apply)
    }.getOrElse(throw new RuntimeException("OrganisationName has not been set in answers"))
  }

  private def detailsList(originalRegistration: Option[RegistrationWrapper])(implicit request: AuthenticatedDataRequest[AnyContent]) = {
      originalRegistration match {
        case Some(registration) =>

          val result = SummaryListViewModel(
            rows = (
              getHasTradingNameRows(registration) ++
                getTradingNameRows(registration) ++
                getBusinessContactDetailsRows(registration) ++
                getBankDetailsRows(registration)
              ).flatten
          )
          result
        case None =>
          SummaryListViewModel(rows = Seq.empty)
      }
  }

  private def getHasTradingNameRows(originalRegistration: RegistrationWrapper)
                                   (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.registration.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNames).getOrElse(List.empty)
    val originalNames = originalAnswers.map(_.tradingName)
    val amendedNames = amendedAnswers.map(_.name)

    val hasChangedToNo = amendedNames.isEmpty && originalNames.nonEmpty
    val hasChangedToYes = amendedNames.nonEmpty && originalNames.nonEmpty || originalNames.isEmpty
    val notAmended = amendedNames.nonEmpty && originalNames.nonEmpty || amendedNames.isEmpty && originalNames.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo || hasChangedToYes) {
      Seq(HasTradingNameSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(originalRegistration: RegistrationWrapper)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.registration.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNames).map(_.map(_.name)).getOrElse(List.empty)
    val originalNames = originalAnswers.map(_.tradingName)
    val addedTradingName = amendedAnswers.diff(originalNames)
    val removedTradingNames = originalNames.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original.toString => TradingName(amended)
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

  private def getBusinessContactDetailsRows(originalRegistration: RegistrationWrapper)
                                           (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalContactName = Some(originalRegistration.registration.schemeDetails.contactName)
    val originalTelephone = Some(originalRegistration.registration.schemeDetails.businessTelephoneNumber)
    val originalEmail = Some(originalRegistration.registration.schemeDetails.businessEmailId)
    val amendedUA = request.userAnswers.get(BusinessContactDetailsPage)
    val amendedContactName = amendedUA.map(_.fullName)
    val amendedTelephone = amendedUA.map(_.telephoneNumber)
    val amendedEmail = amendedUA.map(_.emailAddress)

    val contactNameChanged = amendedContactName.exists(_ != originalContactName.getOrElse(""))
    val telephoneChanged = amendedTelephone.exists(_ != originalTelephone.getOrElse(""))
    val emailChanged = amendedEmail.exists(_ != originalEmail.getOrElse(""))

    Seq(
      if (contactNameChanged) {
        BusinessContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (telephoneChanged) {
        BusinessContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (emailChanged) {
        BusinessContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getBankDetailsRows(originalRegistration: RegistrationWrapper)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.registration.bankDetails
    val amendedUA = request.userAnswers.get(BankDetailsPage)

    val originalAccountName = Some(originalAnswers.accountName)
    val originalBic = Some(originalAnswers.bic).flatten
    val originalIban = Some(originalAnswers.iban)
    val amendedAccountName = amendedUA.map(_.accountName)
    val amendedBic = amendedUA.flatMap(_.bic)
    val amendedIban = amendedUA.map(_.iban)

    val accountNameChanged = amendedAccountName.exists(_ != originalAccountName.getOrElse(""))
    val bicChanged = amendedBic.exists(_ != originalBic.getOrElse(""))
    val ibanChanged = amendedIban.exists(_ != originalIban.getOrElse(""))

    Seq(
      if (accountNameChanged) {
        BankDetailsSummary.amendedRowAccountName(request.userAnswers)
      } else {
        None
      },

      if (bicChanged) {
        Some(BankDetailsSummary.amendedRowBIC(request.userAnswers)).flatten
      } else {
        None
      },

      if (ibanChanged) {
        BankDetailsSummary.amendedRowIBAN(request.userAnswers)
      } else {
        None
      }
    )
  }
}
