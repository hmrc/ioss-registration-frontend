/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.amend

import controllers.actions.AuthenticatedControllerComponents
import forms.amend.ViewOrChangePreviousRegistrationFormProvider
import pages.Waypoints
import pages.amend.ViewOrChangePreviousRegistrationPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.amend.ViewOrChangePreviousRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewOrChangePreviousRegistrationController @Inject()(
                                                            override val messagesApi: MessagesApi,
                                                            cc: AuthenticatedControllerComponents,
                                                            formProvider: ViewOrChangePreviousRegistrationFormProvider,
                                                            view: ViewOrChangePreviousRegistrationView
                                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  // TODO -> Need previous registrations -> Can't use authAndRequireIoss() as defaults to current registration

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIoss() {
    implicit request =>

      val iossNumber: String = request.iossNumber

      val form: Form[Boolean] = formProvider(iossNumber)

      val preparedForm = request.userAnswers.get(ViewOrChangePreviousRegistrationPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, iossNumber))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIoss().async {
    implicit request =>

      val iossNumber: String = request.iossNumber

      val form: Form[Boolean] = formProvider(iossNumber)

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, iossNumber)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ViewOrChangePreviousRegistrationPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(ViewOrChangePreviousRegistrationPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
