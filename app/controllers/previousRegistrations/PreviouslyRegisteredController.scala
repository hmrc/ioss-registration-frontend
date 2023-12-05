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

package controllers.previousRegistrations

import controllers.actions.AuthenticatedControllerComponents
import forms.previousRegistrations.PreviouslyRegisteredFormProvider
import pages.Waypoints
import pages.previousRegistrations.PreviouslyRegisteredPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousRegistration.{AllPreviousRegistrationsRawQuery, DeriveNumberOfPreviousRegistrations}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CheckExistingRegistrations.cleanup
import views.html.previousRegistrations.PreviouslyRegisteredView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviouslyRegisteredController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: PreviouslyRegisteredFormProvider,
                                                view: PreviouslyRegisteredView
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend) {
    implicit request =>

      val preparedForm = request.userAnswers.get(PreviouslyRegisteredPage) match {
        case None => form
        case Some(otherOneStopRegistrations: Boolean) =>
          if (waypoints.inAmend && request.hasExistingPreviousEURegistrationDetails) {
            throw new InvalidAmendModeOperationException(
              "Cannot change otherOneStopRegistrations when in amend mode and have existing registrations"
            )
          } else {
            form.fill(otherOneStopRegistrations)
          }
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviouslyRegisteredPage, value))
            finalAnswers <- Future.fromTry(cleanup(updatedAnswers, DeriveNumberOfPreviousRegistrations, AllPreviousRegistrationsRawQuery))
            _ <- cc.sessionRepository.set(finalAnswers)
          } yield Redirect(PreviouslyRegisteredPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
