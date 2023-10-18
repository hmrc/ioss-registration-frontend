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

import config.FrontendAppConfig
import controllers.GetCountry
import controllers.actions.AuthenticatedControllerComponents
import forms.previousRegistrations.PreviousIossNumberFormProvider
import logging.Logging
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.{IntermediaryIdentificationNumberValidation, IossRegistrationNumberValidation}
import models.requests.AuthenticatedDataRequest
import models.{Country, Index, PreviousScheme}
import pages.{JourneyRecoveryPage, Waypoints}
import pages.previousRegistrations.{PreviousIossNumberPage, PreviousIossSchemePage, PreviousSchemePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousIossNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousIossNumberController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: PreviousIossNumberFormProvider,
                                              coreRegistrationValidationService: CoreRegistrationValidationService,
                                              appConfig: FrontendAppConfig,
                                              view: PreviousIossNumberView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>

        getHasIntermediary(waypoints, countryIndex, schemeIndex) { hasIntermediary =>

          val form = formProvider(country, hasIntermediary)

          val preparedForm = request.userAnswers.get(PreviousIossNumberPage(countryIndex, schemeIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Future.successful(Ok(view(
            preparedForm, waypoints, countryIndex, schemeIndex, country, hasIntermediary, getIossHintText(country), getIntermediaryHintText(country))))
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>

        getHasIntermediary(waypoints, countryIndex, schemeIndex) { hasIntermediary =>
          getPreviousScheme(waypoints, countryIndex, schemeIndex) { previousScheme =>

            val form = formProvider(country, hasIntermediary)

            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(
                  formWithErrors, waypoints, countryIndex, schemeIndex, country, hasIntermediary, getIossHintText(country), getIntermediaryHintText(country)))),

              value =>
                coreRegistrationValidationService.searchScheme(
                  searchNumber = value.previousSchemeNumber,
                  previousScheme = previousScheme,
                  intermediaryNumber = value.previousIntermediaryNumber,
                  countryCode = country.code
                ).flatMap {
                  case Some(activeMatch) if activeMatch.matchType.isActiveTrader =>
                    Future.successful(
                      Redirect(controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(
                        waypoints,
                        activeMatch.memberState))
                    )
                  case Some(activeMatch) if activeMatch.matchType.isQuarantinedTrader =>
                    Future.successful(Redirect(controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(waypoints)))
                  case _ =>
                    saveAndRedirect(countryIndex, schemeIndex, value, waypoints)
                }
            )
          }
        }
      }
  }

  private def saveAndRedirect(countryIndex: Index, schemeIndex: Index, previousSchemeNumbers: PreviousSchemeNumbers, waypoints: Waypoints)
                             (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousIossNumberPage(countryIndex, schemeIndex), previousSchemeNumbers))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield Redirect(PreviousIossNumberPage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
  }

  private def getHasIntermediary(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index)
                                (block: Boolean => Future[Result])
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousIossSchemePage(countryIndex, schemeIndex)).map {
      hasIntermediary =>
        block(hasIntermediary)
    }.getOrElse {
      logger.error("Failed to get intermediary")
      Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
    }

  private def getPreviousScheme(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index)
                               (block: PreviousScheme => Future[Result])
                               (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousSchemePage(countryIndex, schemeIndex)).map {
      previousScheme =>
        block(previousScheme)
    }.getOrElse {
      logger.error("Failed to get previous scheme")
      Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
    }

  private def getIossHintText(country: Country): String = {
    IossRegistrationNumberValidation.euCountriesWithIOSSValidationRules.filter(_.country == country).head match {
      case countryWithIossValidation => countryWithIossValidation.messageInput
    }
  }

  private def getIntermediaryHintText(country: Country): String = {
    IntermediaryIdentificationNumberValidation.euCountriesWithIntermediaryValidationRules.filter(_.country == country).head match {
      case countryWithIntermediaryValidation => countryWithIntermediaryValidation.messageInput
    }
  }

}
