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

import controllers.AnswerExtractor
import controllers.actions.AuthenticatedControllerComponents
import forms.euDetails.AddEuDetailsFormProvider
import models.Country
import models.euDetails.EuOptionalDetails
import pages.Waypoints
import pages.euDetails.AddEuDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.DeriveNumberOfEuRegistrations
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CompletionChecks
import utils.EuDetailsCompletionChecks.{getAllIncompleteEuDetails, incompleteCheckEuDetailsRedirect}
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.getDerivedItems
import viewmodels.checkAnswers.euDetails.EuDetailsSummary
import views.html.euDetails.AddEuDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEuDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: AddEuDetailsFormProvider,
                                        view: AddEuDetailsView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>
      getDerivedItems(waypoints, DeriveNumberOfEuRegistrations) {
        number =>

          val canAddEuDetails = number < Country.euCountries.size
          val euDetailsSummary = EuDetailsSummary.countryAndVatNumberList(request.userAnswers, waypoints, AddEuDetailsPage())

          withCompleteDataAsync[EuOptionalDetails](
            data = getAllIncompleteEuDetails _,
            onFailure = (incomplete: Seq[EuOptionalDetails]) => {
              Future.successful(Ok(view(form, waypoints, euDetailsSummary, canAddEuDetails, incomplete)))
            }) {
            Future.successful(Ok(view(form, waypoints, euDetailsSummary, canAddEuDetails)))
          }
      }
  }

  def onSubmit(waypoints: Waypoints, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>
      withCompleteDataAsync[EuOptionalDetails](
        data = getAllIncompleteEuDetails _,
        onFailure = (incomplete: Seq[EuOptionalDetails]) => {
          if (incompletePromptShown) {
            incompleteCheckEuDetailsRedirect(waypoints).map(
              redirectIncompletePage => redirectIncompletePage.toFuture
            ).getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).toFuture)
          } else {
            Future.successful(Redirect(routes.AddEuDetailsController.onPageLoad(waypoints)))
          }
        }) {
        getDerivedItems(waypoints, DeriveNumberOfEuRegistrations) {
          number =>
            val canAddCountries = number < Country.euCountries.size
            form.bindFromRequest().fold(
              formWithErrors => {
                val list = EuDetailsSummary.countryAndVatNumberList(request.userAnswers, waypoints, AddEuDetailsPage())
                Future.successful(BadRequest(view(formWithErrors, waypoints, list, canAddCountries)))
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(AddEuDetailsPage(), value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(AddEuDetailsPage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
        }
      }
  }
}
