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
import forms.euDetails.FixedEstablishmentAddressFormProvider
import models.{Index, InternationalAddress}
import pages.Waypoints
import pages.euDetails.FixedEstablishmentAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.euDetails.FixedEstablishmentAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FixedEstablishmentAddressController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     cc: AuthenticatedControllerComponents,
                                                     formProvider: FixedEstablishmentAddressFormProvider,
                                                     view: FixedEstablishmentAddressView
                                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getCountry(waypoints, countryIndex) {

        country =>

        val form: Form[InternationalAddress] = formProvider(country)
        val preparedForm = request.userAnswers.get(FixedEstablishmentAddressPage(countryIndex)) match {
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

        val form: Form[InternationalAddress] = formProvider(country)
        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, country)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(FixedEstablishmentAddressPage(countryIndex), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(FixedEstablishmentAddressPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
