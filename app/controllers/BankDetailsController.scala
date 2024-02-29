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
import forms.BankDetailsFormProvider
import pages.{BankDetailsPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.BankDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BankDetailsController @Inject()(override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: BankDetailsFormProvider,
                                      view: BankDetailsView)(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  override protected def controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataAndCheckVerifyEmail(waypoints.registrationModificationMode) {
    implicit request =>

      val preparedForm = request.userAnswers.get(BankDetailsPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.registrationModificationMode).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(view(formWithErrors, waypoints)))
        },
        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BankDetailsPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(BankDetailsPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
        }
      )
  }
}
