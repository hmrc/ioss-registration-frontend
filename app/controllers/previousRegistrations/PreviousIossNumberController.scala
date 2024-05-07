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

import controllers.GetCountry
import controllers.actions.{AmendingActiveRegistration, AuthenticatedControllerComponents}
import controllers.previousRegistrations.GetPreviousScheme.getPreviousScheme
import forms.previousRegistrations.PreviousIossNumberFormProvider
import logging.Logging
import models.core.MatchType
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.{IntermediaryIdentificationNumberValidation, IossRegistrationNumberValidation, NonCompliantDetails}
import models.requests.AuthenticatedDataRequest
import models.{Country, Index, PreviousScheme, UserAnswers}
import pages.previousRegistrations.{PreviousIossNumberPage, PreviousIossSchemePage}
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistration.NonCompliantQuery
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousIossNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousIossNumberController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: PreviousIossNumberFormProvider,
                                              coreRegistrationValidationService: CoreRegistrationValidationService,
                                              view: PreviousIossNumberView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] =
    cc.authAndGetData(waypoints.registrationModificationMode).async {
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

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] =
    cc.authAndGetData(waypoints.registrationModificationMode).async {
      implicit request =>
        getPreviousCountry(waypoints, countryIndex) { country =>

          getHasIntermediary(waypoints, countryIndex, schemeIndex) { hasIntermediary =>
            getPreviousScheme(waypoints, countryIndex, schemeIndex) { previousScheme: PreviousScheme =>
              val form = formProvider(country, hasIntermediary)

              val isNotAmendingActiveRegistration = waypoints.registrationModificationMode != AmendingActiveRegistration

              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(
                    formWithErrors, waypoints, countryIndex, schemeIndex, country, hasIntermediary, getIossHintText(country), getIntermediaryHintText(country)))),

                previousSchemeNumbers =>
                  coreRegistrationValidationService.searchScheme(
                    searchNumber = previousSchemeNumbers.previousSchemeNumber,
                    previousScheme = previousScheme,
                    intermediaryNumber = previousSchemeNumbers.previousIntermediaryNumber,
                    countryCode = country.code
                  ).flatMap {
                    case Some(activeMatch) if isNotAmendingActiveRegistration && activeMatch.matchType.isActiveTrader =>
                      Future.successful(
                        Redirect(controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(
                          EmptyWaypoints,
                          activeMatch.memberState))
                      )

                    case Some(activeMatch) if isNotAmendingActiveRegistration && activeMatch.matchType.isQuarantinedTrader =>
                      Future.successful(Redirect(controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(EmptyWaypoints)))

                    case Some(activeMatch) if activeMatch.matchType == MatchType.TransferringMSID =>
                      saveAndRedirect(countryIndex, schemeIndex, previousSchemeNumbers,
                        Some(NonCompliantDetails(activeMatch.nonCompliantPayments, activeMatch.nonCompliantReturns)),
                        waypoints
                      )

                    case _ =>
                      saveAndRedirect(countryIndex, schemeIndex, previousSchemeNumbers, None, waypoints)
                  }
              )
            }
          }
        }
    }

  private def saveAndRedirect(
                               countryIndex: Index,
                               schemeIndex: Index,
                               previousSchemeNumbers: PreviousSchemeNumbers,
                               nonCompliantDetails: Option[NonCompliantDetails],
                               waypoints: Waypoints)
                             (implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousIossNumberPage(countryIndex, schemeIndex), previousSchemeNumbers))
      updatedAnswersWithNonCompliantDetails <- setNonCompliantAnswers(countryIndex, schemeIndex, nonCompliantDetails, updatedAnswers)
      _ <- cc.sessionRepository.set(updatedAnswersWithNonCompliantDetails)
    } yield {
      Redirect(PreviousIossNumberPage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithNonCompliantDetails).route)
    }
  }

  private def setNonCompliantAnswers(
                                      countryIndex: Index,
                                      schemeIndex: Index,
                                      nonCompliantDetails: Option[NonCompliantDetails],
                                      updatedAnswers: UserAnswers
                                    ): Future[UserAnswers] = {
    nonCompliantDetails match {
      case Some(value) =>
        Future.fromTry(updatedAnswers.set(NonCompliantQuery(countryIndex, schemeIndex), value))
      case _ => updatedAnswers.toFuture
    }
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
