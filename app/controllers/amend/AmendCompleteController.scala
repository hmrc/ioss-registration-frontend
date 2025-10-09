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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions._
import logging.Logging
import models.{Country, TradingName, UserAnswers, Website}
import models.domain.PreviousSchemeDetails
import models.etmp.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.euDetails.EuOptionalDetails
import models.requests.AuthenticatedMandatoryIossRequest
import pages.{BankDetailsPage, BusinessContactDetailsPage, JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AllWebsites, OriginalRegistrationQuery, PreviousRegistrationIossNumberQuery}
import queries.euDetails.{AllEuDetailsQuery, AllEuOptionalDetailsQuery}
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.amend.AmendCompleteView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AmendCompleteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         view: AmendCompleteView,
                                         frontendAppConfig: FrontendAppConfig,
                                         registrationConnector: RegistrationConnector
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndRequireIoss(AmendingActiveRegistration, restrictFromPreviousRegistrations = false, waypoints = waypoints).async {

      implicit request => {

        val userResearchUrl = frontendAppConfig.userResearchUrl2

        val iossNumber = request.userAnswers.get(PreviousRegistrationIossNumberQuery).getOrElse(request.iossNumber)
        val ossRegistration = request.latestOssRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations

        for {
          externalEntryUrl <- registrationConnector.getSavedExternalEntry()
        } yield {
          {
            for {
              organisationName <- getOrganisationName(request.userAnswers)
              originalRegistration <- request.userAnswers.get(OriginalRegistrationQuery(iossNumber))
            } yield {
              val savedUrl = externalEntryUrl.fold(_ => None, _.url)
              val list: SummaryList = detailsList(originalRegistration)
              Ok(
                view(
                  request.vrn,
                  frontendAppConfig.feedbackUrl,
                  savedUrl,
                  frontendAppConfig.iossYourAccountUrl,
                  organisationName,
                  list,
                  ossRegistration,
                  numberOfIossRegistrations,
                  userResearchUrl
                )
              )
            }
          }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url))
        }
      }
    }

  private def getOrganisationName(answers: UserAnswers): Option[String] =
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => None
    }

  private def detailsList(originalRegistration: EtmpDisplayRegistration)(implicit request: AuthenticatedMandatoryIossRequest[AnyContent]) = {
    SummaryListViewModel(
      rows = (
        getHasTradingNameRows(originalRegistration) ++
          getTradingNameRows(originalRegistration) ++
          getHasPreviouslyRegistered(originalRegistration) ++
          getPreviouslyRegisteredRows(originalRegistration) ++
          getCountriesWithNewSchemes(originalRegistration) ++
          getHasRegisteredInEuRows(originalRegistration) ++
          getRegisteredInEuRows(originalRegistration) ++
          getChangedEuDetailsRows(originalRegistration) ++
          getWebsitesRows(originalRegistration) ++
          getBusinessContactDetailsRows(originalRegistration) ++
          getBankDetailsRows(originalRegistration)
        ).flatten
    )
  }

  private def getHasTradingNameRows(originalRegistration: EtmpDisplayRegistration)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.tradingNames
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

  private def getTradingNameRows(originalRegistration: EtmpDisplayRegistration)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.tradingNames.map(_.tradingName)
    val amendedAnswers = request.userAnswers.get(AllTradingNames).map(_.map(_.name)).getOrElse(List.empty)
    val addedTradingName = amendedAnswers.diff(originalAnswers)
    val removedTradingNames = originalAnswers.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => TradingName(amended)
    } ++ amendedAnswers.drop(originalAnswers.size).map(tradingName => TradingName(tradingName))

    val addedTradingNameRow = if (addedTradingName.nonEmpty) {
      request.userAnswers.set(AllTradingNames, changedTradingName) match {
        case Success(amendedUserAnswer) =>
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

  private def getHasPreviouslyRegistered(originalRegistration: EtmpDisplayRegistration)
                                        (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
    val amendedAnswers = request.userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(List.empty)
    val hasChangedToNo = amendedAnswers.diff(originalAnswers)
    val hasChangedToYes = originalAnswers.diff(amendedAnswers)
    val notAmended = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || amendedAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo.nonEmpty || hasChangedToYes.nonEmpty) {
      Seq(PreviouslyRegisteredSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }

  }

  private def getPreviouslyRegisteredRows(originalRegistration: EtmpDisplayRegistration)
                                         (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
    val amendedAnswers = request.userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(List.empty)

    val newPreviouslyRegisteredCountry = amendedAnswers.filterNot { addedCountry =>
      originalAnswers.contains(addedCountry)
    }

    if (newPreviouslyRegisteredCountry.nonEmpty) {
      val addedDetails = request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(List.empty)
        .filter(details => newPreviouslyRegisteredCountry.contains(details.previousEuCountry.code))

      request.userAnswers.set(AllPreviousRegistrationsQuery, addedDetails) match {
        case Success(amendedUserAnswers) =>
          Seq(PreviousRegistrationSummary.amendedAnswersRow(answers = amendedUserAnswers))
        case Failure(_) =>
          Seq.empty
      }
    } else {
      Seq.empty
    }

  }

  private def getCountriesWithNewSchemes(originalRegistration: EtmpDisplayRegistration)
                                        (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val amendedDetails = request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(List.empty)
    val registrationDetails = originalRegistration.schemeDetails.previousEURegistrationDetails

    val changedSchemeDetails = amendedDetails.flatMap { amendedCountry =>
      val matchingEuCountry = registrationDetails.filter(_.issuedBy == amendedCountry.previousEuCountry.code)
      val existingSchemeDetails = matchingEuCountry.map { registration =>
        PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registration).previousSchemeNumbers
      }
      val newSchemes = amendedCountry.previousSchemesDetails.map(_.previousSchemeNumbers)

      val hasSchemeNumbersChanged = existingSchemeDetails.nonEmpty && newSchemes != existingSchemeDetails

      if (hasSchemeNumbersChanged) {
        Some(amendedCountry.previousEuCountry)
      } else {
        None
      }
    }

    if (changedSchemeDetails.nonEmpty) {
      Seq(PreviousRegistrationSummary.changedAnswersRow(changedSchemeDetails))
    } else {
      Seq.empty
    }
  }

  private def getHasRegisteredInEuRows(originalRegistration: EtmpDisplayRegistration)
                                      (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.euRegistrationDetails.map(_.issuedBy)
    val amendedAnswers = request.userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(List.empty)
    val hasChangedToNo = amendedAnswers.isEmpty && originalAnswers.nonEmpty
    val hasChangedToYes = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || originalAnswers.isEmpty
    val notAmended = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || amendedAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo || hasChangedToYes) {
      Seq(TaxRegisteredInEuSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getRegisteredInEuRows(originalRegistration: EtmpDisplayRegistration)
                                   (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.euRegistrationDetails.map(_.issuedBy)

    val amendedAnswers = request.userAnswers
      .get(AllEuDetailsQuery)
      .map(_.map(_.euCountry.code))
      .getOrElse(Seq.empty)

    val addedEuDetails = amendedAnswers.diff(originalAnswers)
    val removedEuDetails = originalAnswers.diff(amendedAnswers)

    val newOrChangedEuDetails = amendedAnswers.filterNot { amendedCountry =>
      originalAnswers.contains(amendedCountry)
    }

    val removedEuDetailsCountries: Seq[Country] = removedEuDetails.flatMap(Country.fromCountryCode)

    val addedEuDetailsRow = if (addedEuDetails.nonEmpty) {
      val changedDetails = request.userAnswers.get(AllEuOptionalDetailsQuery).getOrElse(List.empty)
        .filter(details => newOrChangedEuDetails.contains(details.euCountry.code))

      request.userAnswers.set(AllEuOptionalDetailsQuery, changedDetails) match {
        case Success(amendedUserAnswers) =>
          Some(EuDetailsSummary.amendedAnswersRow(amendedUserAnswers))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedEuDetailsRow = Some(EuDetailsSummary.removedAnswersRow(removedEuDetailsCountries))

    Seq(addedEuDetailsRow, removedEuDetailsRow).flatten
  }

  private def getChangedEuDetailsRows(originalRegistration: EtmpDisplayRegistration)
                                     (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val userEuDetails = request.userAnswers.get(AllEuOptionalDetailsQuery).getOrElse(List.empty)
    val registrationEuDetails = originalRegistration.schemeDetails.euRegistrationDetails

    val changedEuDetailCountries: Seq[Country] = userEuDetails.flatMap { userDetail =>
      registrationEuDetails.find(_.issuedBy == userDetail.euCountry.code) match {
        case Some(registrationDetail) if hasDetailsChanged(userDetail, registrationDetail) =>
          Some(userDetail.euCountry)
        case _ =>
          None
      }
    }

    if (changedEuDetailCountries.nonEmpty) {
      Seq(EuDetailsSummary.changedAnswersRow(changedEuDetailCountries))
    } else {
      Seq.empty
    }

  }


  private def getWebsitesRows(originalRegistration: EtmpDisplayRegistration)
                             (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.websites.map(_.websiteAddress)
    val amendedUA = request.userAnswers.get(AllWebsites).map(_.map(_.site)).getOrElse(List.empty)
    val addedWebsites = amendedUA.diff(originalAnswers)
    val removedWebsites = originalAnswers.diff(amendedUA)

    val changedWebsiteAnswers: List[Website] = amendedUA.zip(originalAnswers).collect {
      case (amended, original) if amended != original => Website(amended)
    } ++ amendedUA.drop(originalAnswers.size).map(site => Website(site))

    val addedWebsiteRow = if (addedWebsites.nonEmpty) {
      request.userAnswers.set(AllWebsites, changedWebsiteAnswers) match {
        case Success(amendedUserAnswers) =>
          Some(WebsiteSummary.amendedAnswersRow(amendedUserAnswers))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedWebsiteRow = Some(WebsiteSummary.removedAnswersRow(removedWebsites))
    Seq(addedWebsiteRow, removedWebsiteRow).flatten
  }

  private def getBusinessContactDetailsRows(originalRegistration: EtmpDisplayRegistration)
                                           (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalContactName = originalRegistration.schemeDetails.contactName
    val originalTelephone = originalRegistration.schemeDetails.businessTelephoneNumber
    val originalEmail = originalRegistration.schemeDetails.businessEmailId
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

  private def getBankDetailsRows(originalRegistration: EtmpDisplayRegistration)
                                (implicit request: AuthenticatedMandatoryIossRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.bankDetails
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

  private def hasDetailsChanged(userDetails: EuOptionalDetails, registrationDetails: EtmpDisplayEuRegistrationDetails): Boolean = {
    val vatNumberWithoutCountryCode = userDetails.euVatNumber.map(_.stripPrefix(userDetails.euCountry.code))
    val registrationVatNumber = registrationDetails.vatNumber

    userDetails.fixedEstablishmentTradingName.exists(_ != registrationDetails.fixedEstablishmentTradingName) ||
      userDetails.fixedEstablishmentAddress.exists(address =>
        !registrationDetails.fixedEstablishmentAddressLine1.equals(address.line1) ||
          !registrationDetails.fixedEstablishmentAddressLine2.equals(address.line2) ||
          !registrationDetails.townOrCity.equals(address.townOrCity) ||
          !registrationDetails.regionOrState.equals(address.stateOrRegion) ||
          !registrationDetails.postcode.equals(address.postCode)
      ) ||
      !vatNumberWithoutCountryCode.equals(registrationVatNumber) ||
      !userDetails.euTaxReference.equals(registrationDetails.taxIdentificationNumber)
  }
}
