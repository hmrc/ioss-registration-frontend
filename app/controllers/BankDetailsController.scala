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

package controllers

import connectors.RegistrationConnector
import controllers.actions.*
import forms.BankDetailsFormProvider
import logging.Logging
import models.BankDetails
import models.amend.RegistrationWrapper
import pages.{BankDetailsPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.BankDetailsView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BankDetailsController @Inject()(override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: BankDetailsFormProvider,
                                      view: BankDetailsView,
                                      clock: Clock,
                                      registrationConnector: RegistrationConnector)(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()

  override protected def controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndGetDataAndCheckVerifyEmail(waypoints.registrationModificationMode, restrictFromPreviousRegistrations = false, waypoints = waypoints).async {
      implicit request =>

        val ossRegistration = request.latestOssRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations

            val preparedForm = request.userAnswers.get(BankDetailsPage) match {
              case Some(value) =>
                form.fill(value)
              case None =>
                ossRegistration match {
                  case Some(ossReg) =>
                    form.fill(BankDetails(
                      accountName = ossReg.bankDetails.accountName,
                      bic = ossReg.bankDetails.bic,
                      iban = ossReg.bankDetails.iban
                    ))
                  case None =>
                    form
                }
            }

        getCanRejoin.map { canRejoin =>
          Ok(view(preparedForm, waypoints, ossRegistration, numberOfIossRegistrations, canRejoin))
        }
    }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndGetData(waypoints.registrationModificationMode, restrictFromPreviousRegistrations = false).async {
      implicit request =>

        val ossRegistration = request.latestOssRegistration
        val numberOfIossRegistrations = request.numberOfIossRegistrations

        form.bindFromRequest().fold(
          formWithErrors => {
            getCanRejoin.map { canRejoin =>
              BadRequest(view(formWithErrors, waypoints, ossRegistration, numberOfIossRegistrations, canRejoin))
            }
          },
          value => {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(BankDetailsPage, value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(BankDetailsPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          }
        )
    }

  private def getCanRejoin(implicit hc: HeaderCarrier): Future[Boolean] = {
    val date = LocalDate.now(clock)

    registrationConnector.getRegistration().map {
      case Right(registrationWrapper) => registrationWrapper.registration.canRejoinRegistration(date)
      case Left(_) => false
    }
  }
}
