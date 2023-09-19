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
import forms.DeleteAllPreviousRegistrationsFormProvider
import models.requests.AuthenticatedDataRequest
import pages.{DeleteAllPreviousRegistrationsPage, Waypoints}
import pages.previousRegistrations.PreviouslyRegisteredPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistration.AllPreviousRegistrationsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DeleteAllPreviousRegistrationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteAllPreviousRegistrationsController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          cc: AuthenticatedControllerComponents,
                                                          formProvider: DeleteAllPreviousRegistrationsFormProvider,
                                                          view: DeleteAllPreviousRegistrationsView
                                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val preparedForm = request.userAnswers.get(DeleteAllPreviousRegistrationsPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>

          determineRemoveAllPreviousRegistrationsAndRedirect(waypoints, value)
      )
  }

  private def determineRemoveAllPreviousRegistrationsAndRedirect(
                                                                  waypoints: Waypoints,
                                                                  value: Boolean
                                                                )(implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    val removeAllPreviousRegistrations = if(value) {
      request.userAnswers.remove(AllPreviousRegistrationsQuery)
    } else {
      request.userAnswers.set(PreviouslyRegisteredPage, true)
    }
    for {
      updatedAnswers <- Future.fromTry(removeAllPreviousRegistrations)
      calculatedAnswers <- Future.fromTry(updatedAnswers.set(DeleteAllPreviousRegistrationsPage, value))
      _ <- cc.sessionRepository.set(calculatedAnswers)
    } yield Redirect(DeleteAllPreviousRegistrationsPage.navigate(waypoints, request.userAnswers, calculatedAnswers).route)
  }
}
