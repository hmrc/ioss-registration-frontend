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

package controllers.previousRegistrations

import controllers.GetCountry
import controllers.actions._
import forms.previousRegistrations.PreviousOssNumberFormProvider
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.PreviousSchemeHintText
import models.requests.AuthenticatedDataRequest
import models.{Country, CountryWithValidationDetails, Index, PreviousScheme, WithName}
import pages.Waypoints
import pages.previousRegistrations.{PreviousOssNumberPage, PreviousSchemePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistration.AllPreviousSchemesForCountryWithOptionalVatNumberQuery
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.previousRegistrations.PreviousOssNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousOssNumberController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             coreRegistrationValidationService: CoreRegistrationValidationService,
                                             formProvider: PreviousOssNumberFormProvider,
                                             view: PreviousOssNumberView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] =
    cc.authAndGetData(waypoints.registrationModificationMode).async {
      implicit request =>
        getPreviousCountry(waypoints, countryIndex) {
          country =>

            val maybeCurrentAnswer = request.userAnswers.get(PreviousOssNumberPage(countryIndex, schemeIndex))

            val (isEditingAndAnotherOssScheme, form) = request.userAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(countryIndex)) match {
              case Some(previousSchemeDetails) =>
                val previousSchemes = previousSchemeDetails.flatMap(_.previousScheme)
                val editingOssScheme = previousSchemes.filter(previousScheme => previousScheme == PreviousScheme.OSSU || previousScheme == PreviousScheme.OSSNU)
                val isEditing = maybeCurrentAnswer.isDefined
                val thereIsAnotherOssScheme = editingOssScheme.size > 1
                val isEditingAndSecondSchemeExists = isEditing && thereIsAnotherOssScheme
                val providingForm = if(!isEditingAndSecondSchemeExists) {
                  formProvider(country, Seq.empty)
                } else {
                  formProvider(country, previousSchemes)
                }
                (isEditingAndSecondSchemeExists, providingForm)
              case None =>
                (false, formProvider(country, Seq.empty))
            }

            val previousSchemeHintText = determinePreviousSchemeHintText(countryIndex, maybeCurrentAnswer.isDefined && !isEditingAndAnotherOssScheme)

            val preparedForm = maybeCurrentAnswer match {
              case None => form
              case Some(value) => form.fill(value.previousSchemeNumber)
            }

            CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == country.code) match {
              case Some(countryWithValidationDetails) =>
                Future.successful(Ok(view(preparedForm, waypoints, countryIndex, schemeIndex, countryWithValidationDetails, previousSchemeHintText)))

              case _ =>
                throw new RuntimeException(s"Cannot find country code ${country.code} in euCountriesWithVRNValidationRules")
            }
        }
    }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] =
    cc.authAndGetData(waypoints.registrationModificationMode).async {
      implicit request =>
        getPreviousCountry(waypoints, countryIndex) {
          country =>

            val maybeCurrentAnswer = request.userAnswers.get(PreviousOssNumberPage(countryIndex, schemeIndex))

            val (isEditingAndAnotherOssScheme, form) = request.userAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(countryIndex)) match {
              case Some(previousSchemeDetails) =>
                val previousSchemes = previousSchemeDetails.flatMap(_.previousScheme)
                val editingOssScheme = previousSchemes.filter(previousScheme => previousScheme == PreviousScheme.OSSU || previousScheme == PreviousScheme.OSSNU)
                val isEditing = maybeCurrentAnswer.isDefined
                val thereIsAnotherOssScheme = editingOssScheme.size > 1
                val isEditingAndSecondSchemeExists = isEditing && thereIsAnotherOssScheme
                val providingForm = if(!isEditingAndSecondSchemeExists) {
                  formProvider(country, Seq.empty)
                } else {
                  formProvider(country, previousSchemes)
                }
                (isEditingAndSecondSchemeExists, providingForm)
              case None =>
                (false, formProvider(country, Seq.empty))
            }

            val previousSchemeHintText = determinePreviousSchemeHintText(countryIndex, maybeCurrentAnswer.isDefined && !isEditingAndAnotherOssScheme)

            form.bindFromRequest().fold(
              formWithErrors =>
                CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
                  case countryWithValidationDetails =>
                    Future.successful(BadRequest(view(
                      formWithErrors, waypoints, countryIndex, schemeIndex, countryWithValidationDetails, previousSchemeHintText)))
                },

              value => {
                val previousScheme = if (value.startsWith("EU")) {
                  PreviousScheme.OSSNU
                } else {
                  PreviousScheme.OSSU
                }
                searchSchemeThenSaveAndRedirect(waypoints, countryIndex, schemeIndex, country, value, previousScheme)
              }
            )
        }
    }

  private def searchSchemeThenSaveAndRedirect(
                                               waypoints: Waypoints,
                                               countryIndex: Index,
                                               schemeIndex: Index,
                                               country: Country,
                                               value: String,
                                               previousScheme: WithName with PreviousScheme
                                             )(implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {

    val isNotAmendingActiveRegistration = waypoints.registrationModificationMode != AmendingActiveRegistration

    if (previousScheme == PreviousScheme.OSSU) {
      coreRegistrationValidationService.searchScheme(
        searchNumber = value,
        previousScheme = previousScheme,
        intermediaryNumber = None,
        countryCode = country.code
      ).flatMap {
        case Some(activeMatch) if isNotAmendingActiveRegistration && activeMatch.matchType.isQuarantinedTrader =>
          Future.successful(Redirect(controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(waypoints)))

        case _ =>
          saveAndRedirect(countryIndex, schemeIndex, value, previousScheme, waypoints)
      }
    } else {
      saveAndRedirect(countryIndex, schemeIndex, value, previousScheme, waypoints)
    }
  }

  private def saveAndRedirect(
                               countryIndex: Index,
                               schemeIndex: Index,
                               registrationNumber: String,
                               previousScheme: PreviousScheme,
                               waypoints: Waypoints
                             )(implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(
        PreviousOssNumberPage(countryIndex, schemeIndex),
        PreviousSchemeNumbers(registrationNumber, None),
      ))
      updatedAnswersWithScheme <- Future.fromTry(updatedAnswers.set(
        PreviousSchemePage(countryIndex, schemeIndex),
        previousScheme
      ))
      _ <- cc.sessionRepository.set(updatedAnswersWithScheme)
    } yield Redirect(PreviousOssNumberPage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithScheme).route)
  }

  private def determinePreviousSchemeHintText(
                                               countryIndex: Index,
                                               hasCurrentAnswer: Boolean
                                             )(implicit request: AuthenticatedDataRequest[AnyContent]): PreviousSchemeHintText = {
    if (hasCurrentAnswer) {
      PreviousSchemeHintText.Both
    } else {
      request.userAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(countryIndex)) match {
        case Some(listSchemeDetails) =>
          val previousSchemes = listSchemeDetails.flatMap(_.previousScheme)
          if (previousSchemes.contains(PreviousScheme.OSSU)) {
            PreviousSchemeHintText.OssNonUnion
          } else if (previousSchemes.contains(PreviousScheme.OSSNU)) {
            PreviousSchemeHintText.OssUnion
          } else {
            PreviousSchemeHintText.Both
          }
        case _ => PreviousSchemeHintText.Both
      }
    }
  }
}

