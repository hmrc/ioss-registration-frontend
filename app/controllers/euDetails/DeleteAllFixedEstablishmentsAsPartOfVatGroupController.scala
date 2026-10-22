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

import controllers.actions.*
import controllers.amend.routes
import logging.Logging
import models.audit.AmendRegistrationAuditModel
import models.audit.RegistrationAuditType.AmendRegistration
import models.audit.SubmissionResult.{Failure, Success}
import models.euDetails.EuDetails
import pages.EmptyWaypoints
import pages.amend.ChangeRegistrationPage
import pages.euDetails.TaxRegisteredInEuPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PreviousRegistrationIossNumberQuery
import queries.euDetails.AllEuDetailsQuery
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel
import views.html.euDetails.DeleteAllFixedEstablishmentsAsPartOfVatGroupView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteAllFixedEstablishmentsAsPartOfVatGroupController @Inject()(
                                                                        override val messagesApi: MessagesApi,
                                                                        cc: AuthenticatedControllerComponents,
                                                                        registrationService: RegistrationService,
                                                                        auditService: AuditService,
                                                                        view: DeleteAllFixedEstablishmentsAsPartOfVatGroupView
                                                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = (cc.authAndGetDataBase(AmendingActiveRegistration) andThen cc.requireIoss()).async {
    implicit request =>
      
      registrationService.toUserAnswers(request.userId, request.registrationWrapper, removeFe = true).flatMap { answers =>
        val euDetailsList: List[EuDetails] = answers.get(AllEuDetailsQuery).getOrElse(List.empty)
        val viewModel: DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel = DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel(euDetailsList)

        for {
          _ <- cc.sessionRepository.set(answers)
        } yield {
          Ok(view(viewModel))
        }
      }
  }

  def onSubmit: Action[AnyContent] = (cc.authAndGetDataBase(AmendingActiveRegistration) andThen cc.requireIoss()).async {
    implicit request =>

      val iossNumber: String = request.userAnswers.get(PreviousRegistrationIossNumberQuery).getOrElse(request.iossNumber)

      (for {
        updatedAnswers <- Future.fromTry(request.userAnswers.remove(AllEuDetailsQuery))
        noEuRegistrationsAnswers <- Future.fromTry(updatedAnswers.set(TaxRegisteredInEuPage, false))
        _ <- cc.sessionRepository.set(noEuRegistrationsAnswers)
      } yield {
        registrationService.amendRegistration(
          answers = request.userAnswers,
          registration = request.registrationWrapper.registration,
          vrn = request.vrn,
          iossNumber = iossNumber,
          rejoin = false
        ).map {
          case Right(response) =>
            auditService.audit(
              AmendRegistrationAuditModel.build(
                registrationAuditType = AmendRegistration,
                userAnswers = request.userAnswers,
                amendRegistrationResponse = Some(response),
                submissionResult = Success
              )(request.request)
            )
            Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
          case Left(e) =>
            logger.error(s"Unexpected result on submitting amend: ${e.body}")
            auditService.audit(
              AmendRegistrationAuditModel.build(
                registrationAuditType = AmendRegistration,
                userAnswers = request.userAnswers,
                amendRegistrationResponse = None,
                submissionResult = Failure
              )(request.request)
            )
            Redirect(routes.ErrorSubmittingAmendmentController.onPageLoad())
        }
      }).flatten
  }
}
