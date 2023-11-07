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

package controllers.euDetails

import controllers.GetCountry
import controllers.actions.AuthenticatedControllerComponents
import forms.euDetails.EuVatNumberFormProvider
import models.{CountryWithValidationDetails, Index}
import pages.Waypoints
import pages.euDetails.EuVatNumberPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuVatNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatNumberController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: EuVatNumberFormProvider,
                                       view: EuVatNumberView,
                                       coreRegistrationValidationService: CoreRegistrationValidationService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

          val form: Form[String] = formProvider(country)
          val preparedForm = request.userAnswers.get(EuVatNumberPage(countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
            case countryWithValidationDetails =>
              Ok(view(preparedForm, waypoints, countryIndex, countryWithValidationDetails)).toFuture
          }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

          val form: Form[String] = formProvider(country)
          form.bindFromRequest().fold(
            formWithErrors =>
              CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
                case countryWithValidationDetails =>
                  BadRequest(view(formWithErrors, waypoints, countryIndex, countryWithValidationDetails)).toFuture
              },

            value =>

              coreRegistrationValidationService.searchEuVrn(value, country.code).flatMap {

                case Some(activeMatch) if activeMatch.matchType.isActiveTrader =>
                  Future.successful(
                    Redirect(
                      controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(
                        waypoints,
                        countryIndex
                      )
                    )
                  )

                case Some(activeMatch) if activeMatch.matchType.isQuarantinedTrader && waypoints.inAmend =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage(countryIndex), value))
                    _ <- cc.sessionRepository.set(updatedAnswers)
                  } yield Redirect(EuVatNumberPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)

                case Some(activeMatch) if activeMatch.matchType.isQuarantinedTrader =>
                  Future.successful(Redirect(controllers.euDetails.routes.ExcludedVRNController.onPageLoad()))

                case _ => for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage(countryIndex), value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(EuVatNumberPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              }
          )
      }
  }
}
