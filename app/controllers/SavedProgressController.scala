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

import config.FrontendAppConfig
import connectors.{RegistrationConnector, SaveForLaterConnector, SavedUserAnswers}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.UserAnswers
import models.requests.SaveForLaterRequest
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SavedProgressView

import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                         cc: AuthenticatedControllerComponents,
                                         view: SavedProgressView,
                                         connector: SaveForLaterConnector,
                                         appConfig: FrontendAppConfig,
                                         registrationConnector: RegistrationConnector,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(continueUrl: RedirectUrl): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val answersExpiry = request.userAnswers.lastUpdated.plus(appConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(clock.getZone).toLocalDate.format(dateTimeFormatter)
      val eventualAnswers = Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl.get(OnlyRelative).url))
      eventualAnswers.failed.foreach(error => logger.error("Failed saving answers", error))

      eventualAnswers.flatMap {
        updatedAnswers: UserAnswers =>
          val s4LRequest = SaveForLaterRequest(updatedAnswers.data, request.vrn)
          (for {
            savedExternalEntry <- registrationConnector.getSavedExternalEntry()
            s4laterResult <- connector.submit(s4LRequest)
          } yield {
            val externalUrl = savedExternalEntry.fold(_ => None, _.url)
            (s4laterResult, externalUrl)
          }).flatMap {
            case (Right(Some(_: SavedUserAnswers)), externalUrl) =>
              for {
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield {
                Ok(view(answersExpiry, continueUrl.get(OnlyRelative).url, externalUrl))
              }
            case (Left(e), _) =>
              logger.error(s"Unexpected result on submit: ${e.toString}")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            case (Right(None), _) =>
              logger.error(s"Unexpected result on submit, no saved for later result and no external url")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      }
  }
}
