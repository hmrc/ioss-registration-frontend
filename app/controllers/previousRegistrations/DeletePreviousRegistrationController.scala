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
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.previousRegistrations.PreviousRegistrationDetailsWithOptionalVatNumber
import models.requests.AuthenticatedDataRequest
import models.Index
import pages.{JourneyRecoveryPage, Waypoints}
import pages.previousRegistrations.DeletePreviousRegistrationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistration.{PreviousRegistrationQuery, PreviousRegistrationWithOptionalVatNumberQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.DeletePreviousRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeletePreviousRegistrationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: DeletePreviousRegistrationFormProvider,
                                        view: DeletePreviousRegistrationView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getPreviousRegistration(waypoints, index) {
        details =>

        Future.successful(Ok(view(form, waypoints, index, details.previousEuCountry.name)))
      }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getPreviousRegistration(waypoints, index) {
        details =>
          saveAndRedirect(waypoints, index, details.previousEuCountry.name)
      }
  }

  private def saveAndRedirect(
                               waypoints: Waypoints,
                               index: Index,
                               countryName: String)
                             (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, waypoints, index, countryName))),

      value =>
        if (value) {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(PreviousRegistrationQuery(index)))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(DeletePreviousRegistrationPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        } else {
          Future.successful(Redirect(DeletePreviousRegistrationPage(index).navigate(waypoints, request.userAnswers, request.userAnswers).route))
        }
    )
  }


  private def getPreviousRegistration(waypoints: Waypoints, index: Index)
                                     (block: PreviousRegistrationDetailsWithOptionalVatNumber => Future[Result])
                                     (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousRegistrationWithOptionalVatNumberQuery(index)).map {
      details =>
        block(details)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)

}
