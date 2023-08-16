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
import queries.VatApiCallResultQuery
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AuthController @Inject()(
                                cc: AuthenticatedControllerComponents,
                                registrationConnector: RegistrationConnector,
                                config: FrontendAppConfig,
                                clock: Clock
                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onSignIn(): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      val answers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId, lastUpdated = Instant.now(clock)))
      answers.get(VatApiCallResultQuery) match {
        case Some(vatApiCallResult) if vatApiCallResult == VatApiCallResult.Success =>
          Redirect(???) .toFuture// to check vat details when created

        case _ =>
          registrationConnector.getVatCustomerInfo().flatMap {
            case Right(vatInfo) =>
              for {
                updatedAnswers <- Future.fromTry(answers.copy(vatInfo = Some(vatInfo)).set(VatApiCallResultQuery, VatApiCallResult.Success))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(???) // to check vat details when created

            case _ =>
              for {
                updatedAnswers <- Future.fromTry(answers.set(VatApiCallResultQuery, VatApiCallResult.Error))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(???) // to vat api down when created
          }
      }
  }

  def signOut(): Action[AnyContent] = Action {
    _ =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
  }

  def signOutNoSurvey(): Action[AnyContent] = Action {
    _ =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(routes.SignedOutController.onPageLoad.url)))
  }
}
