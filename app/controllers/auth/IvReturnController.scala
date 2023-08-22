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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.iv._

import javax.inject.Inject

class IvReturnController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    val controllerComponents: MessagesControllerComponents,
                                    errorView: IvErrorView,
                                    incompleteView: IvIncompleteView,
                                    insufficientEvidenceView: InsufficientEvidenceView,
                                    ivLockedOutView: IvLockedOutView,
                                    ivPreconditionFailedView: IvPreconditionFailedView,
                                    ivTechnicalIssueView: IvTechnicalIssueView,
                                    ivTimeoutView: IvTimeoutView,
                                    ivUserAbortedView: IvUserAbortedView,
                                    ivNotEnoughEvidenceView: IvNotEnoughEvidenceView,
                                    failedMatchingView: IvFailedMatchingView,
                                    ivFailedView: IvFailedView
                                  ) extends FrontendBaseController with I18nSupport {

  def error: Action[AnyContent] = Action {
    implicit request =>
      Ok(errorView())
  }

  def incomplete(): Action[AnyContent] = Action {
    implicit request =>
      Ok(incompleteView())
  }

  def insufficientEvidence(): Action[AnyContent] = Action {
    implicit request =>
      Ok(insufficientEvidenceView())
  }

  def lockedOut(): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivLockedOutView())
  }

  def preconditionFailed(): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivPreconditionFailedView())
  }

  def technicalIssue(): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivTechnicalIssueView())
  }

  def timeout(): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivTimeoutView())
  }

  def userAborted(continueUrl: String): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivUserAbortedView(continueUrl))
  }

  def notEnoughEvidenceSources(): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivNotEnoughEvidenceView())
  }

  def failedMatching(continueUrl: String): Action[AnyContent] = Action {
    implicit request =>
      Ok(failedMatchingView(continueUrl))
  }

  def failed(continueUrl: String): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivFailedView(continueUrl))
  }
}