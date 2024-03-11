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

import controllers.AnswerExtractor
import controllers.actions.AuthenticatedControllerComponents
import forms.euDetails.DeleteEuDetailsFormProvider
import models.Index
import pages.Waypoints
import pages.euDetails.DeleteEuDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.EuDetailsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.euDetails.DeleteEuDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteEuDetailsController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: DeleteEuDetailsFormProvider,
                                           view: DeleteEuDetailsView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.registrationModificationMode) {
    implicit request =>
      getAnswer(EuDetailsQuery(countryIndex)) {
        euDetails =>

          val form: Form[Boolean] = formProvider(euDetails.euCountry)

          Ok(view(form, waypoints, countryIndex, euDetails.euCountry))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.registrationModificationMode).async {
    implicit request =>
      getAnswerAsync(EuDetailsQuery(countryIndex)) {
        euDetails =>

          val form: Form[Boolean] = formProvider(euDetails.euCountry)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, countryIndex, euDetails.euCountry)).toFuture,

            value =>
              if (value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(EuDetailsQuery(countryIndex)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteEuDetailsPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              } else {
                Redirect(DeleteEuDetailsPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
              }
          )
      }
  }
}
