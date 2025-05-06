/*
 * Copyright 2024 HM Revenue & Customs
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

package services.oss

import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.ossRegistration.OssRegistration
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OssRegistrationService @Inject()(
                                           registrationConnector: RegistrationConnector,
                                           config: FrontendAppConfig
                                         )(implicit ec: ExecutionContext) extends Logging {

  def getLatestOssRegistration(enrolments: Enrolments, vrn: Vrn)(implicit hc: HeaderCarrier): Future[Option[OssRegistration]] = {
    val hasOssEnrolment = enrolments.enrolments.exists(_.key == config.ossEnrolment)
    if(hasOssEnrolment) {
      registrationConnector.getOssRegistration(vrn).map {
        case Right(ossRegistration) => Some(ossRegistration)
        case Left(_) => None
      }
    } else {
      None.toFuture
    }
  }
}
