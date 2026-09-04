/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.SaveForLaterConnector
import controllers.actions.*
import forms.ContinueRegistrationFormProvider
import models.ContinueRegistration
import models.requests.AuthenticatedDataRequest
import pages.{JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.core.CoreSavedAnswersRevalidationService
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContinueRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         saveForLaterConnector: SaveForLaterConnector,
                                         formProvider: ContinueRegistrationFormProvider,
                                         view: ContinueRegistrationView,
                                         coreSavedAnswersRevalidationService: CoreSavedAnswersRevalidationService
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataForSavedRegistration() {
    implicit request =>
        request.userAnswers.get(SavedProgressPage).map(
          _ => Ok(view(form))
        ).getOrElse(
          Redirect(controllers.routes.IndexController.onPageLoad())
        )
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataForSavedRegistration().async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),
        value =>
          (value, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) =>
              coreSavedAnswersRevalidationService.checkAndValidateSavedUserAnswers(waypoints).flatMap {
                case Some(redirectResult) =>
                  deleteSavedRegistration(redirectResult)
                case None =>
                  Future.successful(Redirect(Call(GET, url)))
              }

            case (ContinueRegistration.Delete, _) =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveForLaterConnector.delete()
              } yield Redirect(controllers.routes.IndexController.onPageLoad())
            case _ =>
              Future.successful(Redirect(JourneyRecoveryPage.route(waypoints).url))
          }
      )
  }

  private def deleteSavedRegistration(
                                       redirect: Result
                                     )(implicit request: AuthenticatedDataRequest[_]): Future[Result] = {
    for {
      _ <- cc.sessionRepository.clear(request.userId)
      _ <- saveForLaterConnector.delete()
    } yield redirect
  }
}
