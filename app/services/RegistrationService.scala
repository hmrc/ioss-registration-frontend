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

package services

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.RegistrationResultResponse
import logging.Logging
import models.UserAnswers
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import services.etmp.{EtmpEuRegistrations, EtmpPreviousEuRegistrations}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.Future

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with EtmpPreviousEuRegistrations with Logging {

  def createRegistrationRequest(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    val commencementDate = LocalDateTime.now(clock)
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, vrn, commencementDate))
  }
}

