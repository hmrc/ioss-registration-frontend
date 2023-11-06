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
import controllers.AnswerExtractor
import controllers.actions._
import forms.tradingNames.AddTradingNameFormProvider
import pages.Waypoints
import pages.tradingNames.AddTradingNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.tradingNames.DeriveNumberOfTradingNames
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.getDerivedItems
import viewmodels.checkAnswers.tradingName.TradingNameSummary
import views.html.tradingNames.AddTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTradingNameController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: AddTradingNameFormProvider,
                                          view: AddTradingNameView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getDerivedItems(waypoints, DeriveNumberOfTradingNames) {
        number =>

          val canAddTradingNames = number < maxTradingNames
          val tradingNamesSummary = TradingNameSummary.addToListRows(request.userAnswers, waypoints, AddTradingNamePage())

          Ok(view(form, waypoints, tradingNamesSummary, canAddTradingNames)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getDerivedItems(waypoints, DeriveNumberOfTradingNames) {
        number =>

          val canAddTradingNames = number < maxTradingNames
          val tradingNamesSummary = TradingNameSummary.addToListRows(request.userAnswers, waypoints, AddTradingNamePage())

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, tradingNamesSummary, canAddTradingNames)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddTradingNamePage(), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddTradingNamePage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }
}
