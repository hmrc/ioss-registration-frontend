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
import logging.Logging
import models.domain.VatCustomerInfo
import models.{UserAnswers, VatApiCallResult}
import pages.checkVatDetails.CheckVatDetailsPage
import pages.filters.{BusinessBasedInNiPage, NorwegianBasedBusinessPage}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import pages._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.VatApiCallResultQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.time.{Clock, Instant, LocalDate}
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
                                frontendAppConfig: FrontendAppConfig,
                                clock: Clock
                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(frontendAppConfig.allowedRedirectUrls: _*)

  def onSignIn(): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      val answers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId, lastUpdated = Instant.now(clock)))
      answers.get(VatApiCallResultQuery) match {
        case Some(vatApiCallResult) if vatApiCallResult == VatApiCallResult.Success =>
          Redirect(CheckVatDetailsPage.route(EmptyWaypoints).url).toFuture

        case _ =>
          registrationConnector.getVatCustomerInfo().flatMap {
            case Right(vatInfo) if checkVrnExpired(vatInfo) =>
              Redirect(ExpiredVrnDatePage.route(EmptyWaypoints).url).toFuture
            case Right(vatInfo) if checkNiOrNorwayAndRedirect(vatInfo, answers) =>
                checkNiORNorwayAndRedirect(answers)
            case Right(vatInfo) if isNETP(vatInfo) =>
              Redirect(CannotRegisterNonEstablishedTaxablePersonPage.route(EmptyWaypoints).url).toFuture
            case Right(vatInfo) =>
                saveAndRedirect(answers, Some(vatInfo))
            case _ =>
              saveAndRedirect(answers, None)
          }
      }
  }

  private def checkNiOrNorwayAndRedirect(vatInfo: VatCustomerInfo, answers: UserAnswers): Boolean = {
    (vatInfo.singleMarketIndicator, answers.get(BusinessBasedInNiPage)) match {
      case (true, Some(true)) => false
      case (_, Some(true)) => true
      case _ if isNorwegianBasedBusiness(answers, vatInfo) => false
      case _ => true
    }
  }

  private def checkNiORNorwayAndRedirect(answers: UserAnswers): Future[Result] = {
    if(answers.get(BusinessBasedInNiPage).contains(true)) {
      Redirect(CannotRegisterNoNiProtocolPage.route(EmptyWaypoints).url).toFuture
    } else {
      Redirect(CannotRegisterNotNorwegianBasedBusinessPage.route(EmptyWaypoints).url).toFuture
    }
  }

  private def isNETP(vatInfo: VatCustomerInfo): Boolean = {
    (vatInfo.singleMarketIndicator, vatInfo.overseasIndicator, vatInfo.desAddress.countryCode.contains("NO")) match {
      case (true, true, false) => true
      case (false, false, false) => true
      case _ => false
    }
  }

  private def checkVrnExpired(vatInfo: VatCustomerInfo): Boolean =
    vatInfo.deregistrationDecisionDate.exists(!_.isAfter(LocalDate.now(clock)))

  private def saveAndRedirect(answers: UserAnswers, vatInfo: Option[VatCustomerInfo]): Future[Result] = {

    val (updateUAWithVatInfo, page) = if (vatInfo.isDefined) {
      (
        Future.fromTry(answers.copy(vatInfo = vatInfo).set(VatApiCallResultQuery, VatApiCallResult.Success)),
        CheckVatDetailsPage
      )
    } else {
      (
        Future.fromTry(answers.set(VatApiCallResultQuery, VatApiCallResult.Error)),
        VatApiDownPage
      )
    }


    for {
      updatedAnswers <- updateUAWithVatInfo
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield Redirect(page.route(EmptyWaypoints).url)
  }

  private def isNorwegianBasedBusiness(answers: UserAnswers, vatCustomerInfo: VatCustomerInfo): Boolean =
    (vatCustomerInfo.desAddress.countryCode, answers.get(NorwegianBasedBusinessPage)) match {
      case ("NO", Some(true)) => true
      case _ => false
    }

  def redirectToRegister(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    Redirect(
      config.registerUrl,
      Map(
        "origin" -> Seq(config.origin),
        "continueUrl" -> Seq(continueUrl.get(redirectPolicy).url),
        "accountType" -> Seq("Organisation"))
    )
  }

  def redirectToLogin(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    Redirect(config.loginUrl,
      Map(
        "origin" -> Seq(config.origin),
        "continue" -> Seq(continueUrl.get(redirectPolicy).url)
      )
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

  def unsupportedAuthProvider(continueUrl: RedirectUrl): Action[AnyContent] = Action {
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
