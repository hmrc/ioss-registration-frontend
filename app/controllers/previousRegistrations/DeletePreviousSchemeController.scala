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

import config.Constants.lastSchemeForCountry
import controllers.GetCountry
import controllers.actions.AuthenticatedControllerComponents
import forms.previousRegistrations.DeletePreviousSchemeFormProvider
import models.Index
import pages.Waypoints
import pages.previousRegistrations.DeletePreviousSchemePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousRegistration.{DeriveNumberOfPreviousSchemes, PreviousSchemeForCountryQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import viewmodels.checkAnswers.previousRegistrations.{DeletePreviousSchemeSummary, PreviousIntermediaryNumberSummary, PreviousSchemeNumberSummary}
import views.html.previousRegistrations.DeletePreviousSchemeView
import viewmodels.govuk.summarylist._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeletePreviousSchemeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: DeletePreviousSchemeFormProvider,
                                        view: DeletePreviousSchemeView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      val isLastPreviousScheme = request.userAnswers.get(DeriveNumberOfPreviousSchemes(countryIndex)).get == lastSchemeForCountry

      getPreviousCountry(waypoints, countryIndex) {
        country =>

          val list =
            SummaryListViewModel(
              rows = Seq(
                DeletePreviousSchemeSummary.row(request.userAnswers, countryIndex, schemeIndex),
                PreviousSchemeNumberSummary.row(request.userAnswers, countryIndex, schemeIndex),
                PreviousIntermediaryNumberSummary.row(request.userAnswers, countryIndex, schemeIndex)
              ).flatten
            )

          val form = formProvider(country)

          val preparedForm = request.userAnswers.get(DeletePreviousSchemePage(countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Future.successful(Ok(view(preparedForm, waypoints, countryIndex, schemeIndex, country, list, isLastPreviousScheme)))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      val isLastPreviousScheme = request.userAnswers.get(DeriveNumberOfPreviousSchemes(countryIndex)).get == lastSchemeForCountry

      getPreviousCountry(waypoints, countryIndex) {
        country =>

          val list =
            SummaryListViewModel(
              rows = Seq(
                DeletePreviousSchemeSummary.row(request.userAnswers, countryIndex, schemeIndex),
                PreviousSchemeNumberSummary.row(request.userAnswers, countryIndex, schemeIndex),
                PreviousIntermediaryNumberSummary.row(request.userAnswers, countryIndex, schemeIndex)
              ).flatten
            )

          val form = formProvider(country)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, waypoints, countryIndex, schemeIndex, country, list, isLastPreviousScheme))),

            value =>
              if (value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(PreviousSchemeForCountryQuery(countryIndex, schemeIndex)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeletePreviousSchemePage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              } else {
                Future.successful(Redirect(DeletePreviousSchemePage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route))
              }
          )
      }
  }
}
