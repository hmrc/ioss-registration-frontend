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

package controllers.tradingNames

import config.Constants.maxTradingNames
import controllers.actions._
import forms.tradingNames.TradingNameFormProvider
import models.{Index, TradingName}
import pages.Waypoints
import pages.tradingNames.TradingNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.tradingNames.TradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TradingNameController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: TradingNameFormProvider,
                                       view: TradingNameView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] =
    (cc.authAndGetData() andThen cc.limitIndex(index, maxTradingNames)) {
      implicit request =>

        val form: Form[String] = formProvider(index, request.userAnswers.get(AllTradingNames()).getOrElse(Seq.empty).map(_.name))

        val preparedForm = request.userAnswers.get(TradingNamePage(index)) match {
          case None => form
          case Some(value) => form.fill(value.name)
        }

        Ok(view(preparedForm, waypoints, index))
    }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] =
    (cc.authAndGetData() andThen cc.limitIndex(index, maxTradingNames)).async {
      implicit request =>

        val form: Form[String] = formProvider(index, request.userAnswers.get(AllTradingNames()).getOrElse(Seq.empty).map(_.name))

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, index)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TradingNamePage(index), TradingName(value)))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(TradingNamePage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
    }
}
