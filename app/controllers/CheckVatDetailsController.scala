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

import controllers.actions._
import forms.CheckVatDetailsFormProvider
import models.CheckVatDetails
import pages.{CheckVatDetailsPage, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.CheckVatDetailsViewModel
import views.html.CheckVatDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckVatDetailsController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: CheckVatDetailsFormProvider,
                                           view: CheckVatDetailsView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  val form: Form[CheckVatDetails] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      request.userAnswers.vatInfo match {
        case Some(vatInfo) =>
          val preparedForm = request.userAnswers.get(CheckVatDetailsPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          val viewModel = CheckVatDetailsViewModel(request.vrn, vatInfo)
          Ok(view(preparedForm, waypoints, viewModel))

        case None =>
          Redirect(JourneyRecoveryPage.route(waypoints))
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      request.userAnswers.vatInfo match {
        case Some(vatInfo) =>
          form.bindFromRequest().fold(
            formWithErrors => {
              val viewModel = CheckVatDetailsViewModel(request.vrn, vatInfo)
              BadRequest(view(formWithErrors, waypoints, viewModel)).toFuture
            },

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckVatDetailsPage, value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CheckVatDetailsPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        case None =>
          Redirect(JourneyRecoveryPage.route(waypoints)).toFuture
      }
  }
}
