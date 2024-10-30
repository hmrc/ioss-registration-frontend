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

package controllers.actions

import config.FrontendAppConfig
import logging.Logging
import models.requests.AuthenticatedIdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.oss.OssExclusionsService
import uk.gov.hmrc.auth.core.EnrolmentIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckRegistrationFilterImpl(
                                   mode: RegistrationModificationMode,
                                   frontendAppConfig: FrontendAppConfig,
                                   ossExclusionsService: OssExclusionsService
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedIdentifierRequest] with Logging {
  override protected def filter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hasQuarantinedOssEnrolment(request).map { isQuarantinedCode4 =>
      (hasIossEnrolment(request), mode, isQuarantinedCode4) match {
        case (_, NotModifyingExistingRegistration | RejoiningRegistration, true) =>
          Some(Redirect(controllers.ossExclusions.routes.CannotRegisterQuarantinedTraderController.onPageLoad()))
        case (true, NotModifyingExistingRegistration, false) =>
          Some(Redirect(controllers.routes.AlreadyRegisteredController.onPageLoad().url))
        case (false, _: ModifyingExistingRegistrationMode, false) =>
          Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url))
        case _ =>
          None
      }
    }
  }

  private def hasIossEnrolment(request: AuthenticatedIdentifierRequest[_]): Boolean = {
    request.enrolments.enrolments.exists(_.key == frontendAppConfig.iossEnrolment)
  }

  private def hasQuarantinedOssEnrolment(request: AuthenticatedIdentifierRequest[_])(implicit hc: HeaderCarrier): Future[Boolean] = {
    getOssEnrolmentsForVrn(request) match {
      case Some(enrolment) =>
        ossExclusionsService.determineOssExclusionStatus(enrolment.value).map { result =>
          result
        }
      case _ =>
        false.toFuture
    }
  }

  private def getOssEnrolmentsForVrn(request: AuthenticatedIdentifierRequest[_]): Option[EnrolmentIdentifier] = {
    request.enrolments.enrolments.filter(_.key == frontendAppConfig.ossEnrolment).toSeq
      .flatMap(_.identifiers.filter(_.key == "VRN").find(_.value.equals(request.vrn.vrn))).headOption
  }
}

class CheckRegistrationFilterProvider @Inject()(
                                                 frontendAppConfig: FrontendAppConfig,
                                                 ossExclusionsService: OssExclusionsService
                                               )(implicit executionContext: ExecutionContext) {

  def apply(inAmend: RegistrationModificationMode): CheckRegistrationFilterImpl = {
    new CheckRegistrationFilterImpl(inAmend, frontendAppConfig, ossExclusionsService)
  }
}
