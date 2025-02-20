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

package controllers.amend

import connectors.RegistrationConnector
import controllers.actions.{AmendingActiveRegistration, AuthenticatedControllerComponents}
import logging.Logging
import pages.Waypoints
import pages.amend.ChangeRegistrationPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.OriginalRegistrationQuery
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartAmendJourneyController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             registrationConnector: RegistrationConnector,
                                             registrationService: RegistrationService,
                                             authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with Logging {
  protected def controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.authAndGetData(AmendingActiveRegistration) andThen cc.requireIoss()).async {
    implicit request =>

      (for {
        registrationWrapperResponse <- registrationConnector.getRegistration()
      } yield {

        registrationWrapperResponse match {
          case Right(registrationWrapper) =>
            for {
              userAnswers <- registrationService.toUserAnswers(request.userId, registrationWrapper)
              originalAnswers <- Future.fromTry(userAnswers.set(OriginalRegistrationQuery(request.iossNumber), registrationWrapper.registration))
              _ <- authenticatedUserAnswersRepository.set(userAnswers)
              _ <- authenticatedUserAnswersRepository.set(originalAnswers)
            } yield Redirect(ChangeRegistrationPage.route(waypoints).url)
          case Left(error) =>
            val exception = new Exception(error.body)
            logger.error(exception.getMessage, exception)
            throw exception
        }
      }).flatten
  }
}
