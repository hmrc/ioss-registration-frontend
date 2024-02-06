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

package controllers.amend

import config.FrontendAppConfig
import controllers.actions._
import forms.amend.CancelAmendRegFormProvider
import logging.Logging
import pages.Waypoints
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.CancelAmendRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CancelAmendRegistrationController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   formProvider: CancelAmendRegFormProvider,
                                                   appConfig: FrontendAppConfig,
                                                   view: CancelAmendRegistrationView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(AmendingActiveRegistration) {
    implicit request =>

      Ok(view(form, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(AmendingActiveRegistration).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>
          if (value) {
            for {
              _ <- cc.sessionRepository.clear(request.userId)
            } yield Redirect(appConfig.iossYourAccountUrl)
          } else {
            Future.successful(Redirect(ChangeRegistrationPage.route(waypoints).url))
          }
      )
  }
}
