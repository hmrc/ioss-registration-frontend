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
import models.euDetails.EuOptionalDetails
import pages.Waypoints
import pages.euDetails.CheckEuDetailsAnswersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CompletionChecks
import utils.EuDetailsCompletionChecks.{getIncompleteEuDetails, incompleteCheckEuDetailsRedirect}
import viewmodels.checkAnswers.euDetails._
import viewmodels.govuk.summarylist._
import views.html.euDetails.CheckEuDetailsAnswersView

import javax.inject.Inject
import scala.concurrent.Future

class CheckEuDetailsAnswersController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 view: CheckEuDetailsAnswersView
                                               ) extends FrontendBaseController with CompletionChecks with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
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

          Future.successful(withCompleteDataModel[EuOptionalDetails](
            countryIndex,
            data = getIncompleteEuDetails _,
            onFailure = (incomplete: Option[EuOptionalDetails]) => {
              Ok(view(waypoints, countryIndex, country, list, incomplete.isDefined))
            }) {
            Ok(view(waypoints, countryIndex, country, list))
          })
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend) {
    implicit request =>
      val incomplete = getIncompleteEuDetails(countryIndex)
      if (incomplete.isEmpty) {
        Redirect(CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route)
      } else {
        if (!incompletePromptShown) {
          Redirect(routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex))
        } else {
          incompleteCheckEuDetailsRedirect(waypoints).map {
            redirectIncompletePage =>
              redirectIncompletePage
          }.getOrElse(Redirect(routes.EuCountryController.onPageLoad(waypoints, countryIndex)))
        }
      }
  }
}
