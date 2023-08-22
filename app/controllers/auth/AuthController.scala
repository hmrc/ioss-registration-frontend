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

package controllers.auth

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.AuthenticatedControllerComponents
import models.{UserAnswers, VatApiCallResult}
import pages.{CheckVatDetailsPage, EmptyWaypoints, VatApiDownPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.VatApiCallResultQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AuthController @Inject()(
                                cc: AuthenticatedControllerComponents,
                                registrationConnector: RegistrationConnector,
                                insufficientEnrolmentsView: InsufficientEnrolmentsView,
                                unsupportedAffinityGroupView: UnsupportedAffinityGroupView,
                                unsupportedAuthProviderView: UnsupportedAuthProviderView,
                                unsupportedCredentialRoleView: UnsupportedCredentialRoleView,
                                config: FrontendAppConfig,
                                clock: Clock
                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onSignIn(): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      val answers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId, lastUpdated = Instant.now(clock)))
      answers.get(VatApiCallResultQuery) match {
        case Some(vatApiCallResult) if vatApiCallResult == VatApiCallResult.Success =>
          Redirect(CheckVatDetailsPage.route(EmptyWaypoints).url).toFuture

        case _ =>
          registrationConnector.getVatCustomerInfo().flatMap {
            case Right(vatInfo) =>
              for {
                updatedAnswers <- Future.fromTry(answers.copy(vatInfo = Some(vatInfo)).set(VatApiCallResultQuery, VatApiCallResult.Success))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CheckVatDetailsPage.route(EmptyWaypoints).url)

            case _ =>
              for {
                updatedAnswers <- Future.fromTry(answers.set(VatApiCallResultQuery, VatApiCallResult.Error))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatApiDownPage.route(EmptyWaypoints).url)
          }
      }
  }

  def redirectToRegister(continueUrl: String): Action[AnyContent] = Action {
    Redirect(config.registerUrl,
      Map(
        "origin"      -> Seq(config.origin),
        "continueUrl" -> Seq(continueUrl),
        "accountType" -> Seq("Organisation"))
    )
  }

  def redirectToLogin(continueUrl: String): Action[AnyContent] = Action {
    Redirect(config.loginUrl,
      Map(
        "origin"   -> Seq(config.origin),
        "continue" -> Seq(continueUrl))
    )
  }

  def signOut(): Action[AnyContent] = Action {
    _ =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
  }

  def signOutNoSurvey(): Action[AnyContent] = Action {
    _ =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(routes.SignedOutController.onPageLoad.url)))
  }

  def unsupportedAffinityGroup(): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedAffinityGroupView())
  }

  def unsupportedAuthProvider(continueUrl: String): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedAuthProviderView(continueUrl))
  }

  def insufficientEnrolments(): Action[AnyContent] = Action {
    implicit request =>
      Ok(insufficientEnrolmentsView())
  }

  def unsupportedCredentialRole(): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedCredentialRoleView())
  }
}
