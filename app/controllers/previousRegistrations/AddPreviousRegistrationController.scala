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

import controllers.actions._
import forms.previousRegistrations.AddPreviousRegistrationFormProvider
import logging.Logging
import models.Country
import models.requests.AuthenticatedDataRequest
import pages.{JourneyRecoveryPage, Waypoints}
import pages.previousRegistrations.AddPreviousRegistrationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistration.DeriveNumberOfPreviousRegistrations
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.previousRegistrations.PreviousRegistrationSummary
import views.html.previousRegistrations.AddPreviousRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddPreviousRegistrationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: AddPreviousRegistrationFormProvider,
                                        view: AddPreviousRegistrationView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getNumberOfPreviousRegistrations(waypoints) {
        number =>

          val canAddCountries = number < Country.euCountries.size

          val previousRegistrations = PreviousRegistrationSummary.row(request.userAnswers, Seq.empty, waypoints)

          Future.successful(Ok(view(form, waypoints, previousRegistrations, canAddCountries)))

      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getNumberOfPreviousRegistrations(waypoints) {
        number =>

          val canAddCountries = number < Country.euCountries.size
          val previousRegistrations = PreviousRegistrationSummary.row(request.userAnswers, Seq.empty, waypoints)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, waypoints, previousRegistrations, canAddCountries))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddPreviousRegistrationPage(), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddPreviousRegistrationPage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }

  private def getNumberOfPreviousRegistrations(waypoints: Waypoints)(block: Int => Future[Result])
                                              (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(DeriveNumberOfPreviousRegistrations).map {
      number =>
        block(number)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
}

