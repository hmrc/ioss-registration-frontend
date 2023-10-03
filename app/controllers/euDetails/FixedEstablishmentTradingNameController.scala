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

import controllers.GetCountry
import controllers.actions.AuthenticatedControllerComponents
import forms.euDetails.FixedEstablishmentTradingNameFormProvider
import models.Index
import pages.Waypoints
import pages.euDetails.FixedEstablishmentTradingNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.euDetails.FixedEstablishmentTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FixedEstablishmentTradingNameController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         cc: AuthenticatedControllerComponents,
                                                         formProvider: FixedEstablishmentTradingNameFormProvider,
                                                         view: FixedEstablishmentTradingNameView
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

          val form: Form[String] = formProvider(country)
          val preparedForm = request.userAnswers.get(FixedEstablishmentTradingNamePage(countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, countryIndex, country)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

          val form: Form[String] = formProvider(country)
          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, countryIndex, country)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(FixedEstablishmentTradingNamePage(countryIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(FixedEstablishmentTradingNamePage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }
}