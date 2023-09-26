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
import models.Index
import pages.Waypoints
import pages.euDetails.CheckEuDetailsAnswersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails._
import viewmodels.govuk.summarylist._
import views.html.euDetails.CheckEuDetailsAnswersView

import javax.inject.Inject

class CheckEuDetailsAnswersController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 view: CheckEuDetailsAnswersView
                                               ) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

          val thisPage = CheckEuDetailsAnswersPage(countryIndex)

          val list = SummaryListViewModel(
            rows = Seq(
              RegistrationTypeSummary.row(request.userAnswers, waypoints, countryIndex, thisPage),
              EuVatNumberSummary.row(request.userAnswers, waypoints, countryIndex, thisPage),
              EuTaxReferenceSummary.row(request.userAnswers, waypoints, countryIndex, thisPage),
              FixedEstablishmentTradingNameSummary.row(request.userAnswers, waypoints, countryIndex, thisPage),
              FixedEstablishmentAddressSummary.row(request.userAnswers, waypoints, countryIndex, thisPage),
              TaxRegisteredInEuSummary.row(request.userAnswers, waypoints, thisPage)
            ).flatten
          )

          Ok(view(waypoints, countryIndex, country, list)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      Redirect(CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route)
  }
}
