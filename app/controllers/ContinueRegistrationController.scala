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

package controllers

import connectors.{RegistrationConnector, SaveForLaterConnector}
import controllers.actions.*
import forms.ContinueRegistrationFormProvider
import models.ContinueRegistration
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import pages.{JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContinueRegistrationView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         registrationConnector: RegistrationConnector,
                                         coreRegistrationValidationService: CoreRegistrationValidationService,
                                         saveForLaterConnector: SaveForLaterConnector,
                                         formProvider: ContinueRegistrationFormProvider,
                                         view: ContinueRegistrationView,
                                         clock: Clock
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataForSavedRegistration().async {
    implicit request =>
        request.userAnswers.get(SavedProgressPage) match {

          case Some(_) =>
            checkRegistrationStatus(waypoints)

          case None =>
            Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
        }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataForSavedRegistration().async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),
        value =>
          (value, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) => Future.successful(Redirect(Call(GET, url)))
            case (ContinueRegistration.Delete, _) =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveForLaterConnector.delete()
              } yield Redirect(controllers.routes.IndexController.onPageLoad())
            case _ => Future.successful(Redirect(JourneyRecoveryPage.route(waypoints).url))
          }
      )
  }

  private def checkRegistrationStatus(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[_]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    registrationConnector.getVatCustomerInfo().flatMap {
      case Right(vatInfo) if checkVrnExpired(vatInfo) =>
        deleteSavedRegistration(Redirect(controllers.routes.ExpiredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints)))

      case Right(_) =>
        coreRegistrationValidationService.searchUkVrn(request.vrn).flatMap {

          case Some(activeMatch) if activeMatch.isActiveTrader =>
            deleteSavedRegistration(
              Redirect(controllers.routes.AlreadyRegisteredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints, activeMatch.memberState))
            )

          case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
            deleteSavedRegistration(
              Redirect(
                controllers.routes.QuarantinedVatCannotBeUsedForSaveAndComeBackController
                  .onPageLoad(waypoints, activeMatch.memberState, activeMatch.getEffectiveDate)
              )
            )

          case _ =>
            Future.successful(Ok(view(form)))
        }

      case _ =>
        Future.successful(Ok(view(form)))
    }
  }

  private def checkVrnExpired(vatInfo: VatCustomerInfo): Boolean = {
    vatInfo.deregistrationDecisionDate.exists(
      !_.isAfter(LocalDate.now(clock))
    )
  }

  private def deleteSavedRegistration(
                                       redirect: Result
                                     )(implicit request: AuthenticatedDataRequest[_]): Future[Result] = {
    for {
      _ <- cc.sessionRepository.clear(request.userId)
      _ <- saveForLaterConnector.delete()
    } yield redirect
  }
}
