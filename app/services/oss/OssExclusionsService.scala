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

import connectors.RegistrationConnector
import logging.Logging
import models.ossExclusions.{ExclusionReason, OssExcludedTrader}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class OssExclusionsService @Inject()(
                                           clock: Clock,
                                           registrationConnector: RegistrationConnector
                                         )(implicit ec: ExecutionContext) extends Logging {

  def determineOssExclusionStatus(vrn: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate: LocalDate = LocalDate.now(clock)
    registrationConnector.getOssRegistration(Vrn(vrn)).map {
      case Right(ossExcludedTrader) =>
        !isQuarantinedAndAfterTwoYears(currentDate, ossExcludedTrader) &&
          ossExcludedTrader.quarantined.getOrElse(false) &&
          ossExcludedTrader.exclusionReason.contains(ExclusionReason.FailsToComply)

      case Left(error) =>
        val exception = new Exception(s"An error occurred whilst retrieving the OSS Excluded Trader with error: $error")
        logger.error(s"Unable to retrieve OSS Excluded Trader with error: ${exception.getMessage}", exception)
        throw exception
    }
  }

  private def isQuarantinedAndAfterTwoYears(currentDate: LocalDate, ossExcludedTrader: OssExcludedTrader): Boolean = {
    ossExcludedTrader.quarantined match {
      case Some(true) =>
        val minimumDate = currentDate.minusYears(2)
        ossExcludedTrader.effectiveDate.getOrElse {
          val exception = new IllegalStateException(s"Expected effective date")
          logger.error(s"Unable to retrieve effective date: ${exception.getMessage}", exception)
          throw exception
        }.isBefore(minimumDate)
      case _ => false
    }
  }
}
