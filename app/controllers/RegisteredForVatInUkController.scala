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
import forms.RegisteredForVatInUkFormProvider
import models.UserAnswers
import navigation.Navigator
import pages.RegisteredForVatInUkPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegisteredForVatInUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredForVatInUkController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: UnauthenticatedControllerComponents,
                                                navigator: Navigator,
                                                formProvider: RegisteredForVatInUkFormProvider,
                                                view: RegisteredForVatInUkView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetOptionalData {
    implicit request =>

      val preparedForm = request.userAnswers.getOrElse(UserAnswers(request.userId)).get(RegisteredForVatInUkPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(UserAnswers(request.userId)).set(RegisteredForVatInUkPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(RegisteredForVatInUkPage, updatedAnswers))
      )
  }
}
