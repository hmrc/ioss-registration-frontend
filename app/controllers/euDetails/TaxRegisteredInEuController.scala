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

package controllers.euDetails

import controllers.actions.AuthenticatedControllerComponents
import forms.euDetails.TaxRegisteredInEuFormProvider
import models.UserAnswers
import pages.Waypoints
import pages.euDetails.TaxRegisteredInEuPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsArray, JsObject}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.{AllEuDetailsRawQuery, DeriveNumberOfEuRegistrations}
import queries.{Derivable, Settable}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.euDetails.TaxRegisteredInEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TaxRegisteredInEuController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: TaxRegisteredInEuFormProvider,
                                             view: TaxRegisteredInEuView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val preparedForm = request.userAnswers.get(TaxRegisteredInEuPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TaxRegisteredInEuPage, value))
            finalAnswers <- Future.fromTry(cleanup(updatedAnswers, DeriveNumberOfEuRegistrations, AllEuDetailsRawQuery))
            _ <- cc.sessionRepository.set(finalAnswers)
          } yield Redirect(TaxRegisteredInEuPage.navigate(waypoints, request.userAnswers, finalAnswers).route)
      )
  }

  private def cleanup(answers: UserAnswers, derivable: Derivable[Seq[JsObject], Int], query: Settable[JsArray]): Try[UserAnswers] = {
    answers.get(derivable) match {
      case Some(n) if n == 0 => answers.remove(query)
      case _ => Try(answers)
    }
  }
}
