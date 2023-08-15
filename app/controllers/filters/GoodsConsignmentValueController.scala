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

package controllers.filters

import controllers.actions._
import forms.filters.GoodsConsignmentValueFormProvider
import pages.Waypoints
import pages.filters.GoodsConsignmentValuePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.filters.GoodsConsignmentValueView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsConsignmentValueController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: UnauthenticatedControllerComponents,
                                                 formProvider: GoodsConsignmentValueFormProvider,
                                                 view: GoodsConsignmentValueView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val preparedForm = request.userAnswers.get(GoodsConsignmentValuePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsConsignmentValuePage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(GoodsConsignmentValuePage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
