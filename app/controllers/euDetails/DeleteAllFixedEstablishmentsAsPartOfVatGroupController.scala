/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.RegistrationConnector
import controllers.actions.*
import logging.Logging
import models.euDetails.EuDetails
import pages.Waypoints
import pages.euDetails.{DeleteAllFixedEstablishmentsAsPartOfVatGroupPage, TaxRegisteredInEuPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.AllEuDetailsQuery
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import viewmodels.DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel
import views.html.euDetails.DeleteAllFixedEstablishmentsAsPartOfVatGroupView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteAllFixedEstablishmentsAsPartOfVatGroupController @Inject()(
                                                                        override val messagesApi: MessagesApi,
                                                                        cc: AuthenticatedControllerComponents,
                                                                        registrationConnector: RegistrationConnector,
                                                                        registrationService: RegistrationService,
                                                                        view: DeleteAllFixedEstablishmentsAsPartOfVatGroupView
                                                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = {
    val modifyingExistingRegistrationMode = if (waypoints.inAmend) {
      AmendingActiveRegistration
    } else {
      RejoiningRegistration
    }

    cc.authAndGetData(modifyingExistingRegistrationMode).async {
      implicit request =>

        registrationConnector.getRegistration().flatMap {
          case Right(registrationWrapper) =>
            registrationService.toUserAnswers(request.userId, registrationWrapper, removeFe = true).flatMap { answers =>

              val euDetailsList: List[EuDetails] = answers.get(AllEuDetailsQuery).getOrElse(List.empty)
              val viewModel: DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel = DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel(euDetailsList)

              Ok(view(waypoints, viewModel)).toFuture
            }
          case Left(error) =>
            val errorMessage: String = s"An error occurred when retrieving registration with error: ${error.body}."
            logger.error(errorMessage, error)
            val exception: Exception = new Exception(errorMessage)
            throw exception
        }
    }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(AmendingActiveRegistration).async {
    implicit request =>

      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.remove(AllEuDetailsQuery))
        noEuRegistrationsAnswers <- Future.fromTry(updatedAnswers.set(TaxRegisteredInEuPage, false))
        _ <- cc.sessionRepository.set(noEuRegistrationsAnswers)
      } yield Redirect(DeleteAllFixedEstablishmentsAsPartOfVatGroupPage.navigate(waypoints, request.userAnswers, noEuRegistrationsAnswers).route)
  }
}
