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
import models.Index
import pages.{NonEmptyWaypoints, Waypoints}
import pages.euDetails.{CannotRegisterFixedEstablishmentOperationOnlyPage, CheckEuDetailsAnswersPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.EuDetailsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.euDetails.CannotRegisterFixedEstablishmentOperationOnlyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotRegisterFixedEstablishmentOperationOnlyController @Inject()(
                                                                         override val messagesApi: MessagesApi,
                                                                         cc: AuthenticatedControllerComponents,
                                                                         view: CannotRegisterFixedEstablishmentOperationOnlyView
                                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = (cc.actionBuilder andThen cc.identify) {
    implicit request =>
      Ok(view(waypoints, countryIndex))
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.registrationModificationMode).async {
    implicit request =>

      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.remove(EuDetailsQuery(countryIndex)))
        _ <- cc.sessionRepository.set(updatedAnswers)
      } yield {
        val updatedWaypoints = waypoints match {
          case w: NonEmptyWaypoints if w.next.page.isTheSamePage(CheckEuDetailsAnswersPage(countryIndex)) =>
            Waypoints(w.waypoints.tail)
          case w => w
        }
        Redirect(CannotRegisterFixedEstablishmentOperationOnlyPage(countryIndex).navigate(updatedWaypoints, request.userAnswers, updatedAnswers).route)
      }
  }
}
