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
import models.ossRegistration.OssRegistration
import models.requests.AuthenticatedIdentifierRequest
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{Request, Result}
import services.oss.OssRegistrationService
import services.{AccountService, UrlBuilderService}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthenticatedIdentifierAction(ossRegistration: Option[OssRegistration], numberOfIossRegistrations: Int) extends AuthenticatedIdentifierAction(
  mock[AuthConnector],
  mock[FrontendAppConfig],
  mock[UrlBuilderService],
  mock[AccountService],
  mock[OssRegistrationService]
)(ExecutionContext.Implicits.global) {

  override def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedIdentifierRequest[A]]] =
    Right(AuthenticatedIdentifierRequest(
      request,
      Credentials("12345-credId", "GGW"),
      Vrn("123456789"),
      Enrolments(Set.empty),
      Some("IM9001234567"),
      numberOfIossRegistrations,
      ossRegistration
    )).toFuture
}
