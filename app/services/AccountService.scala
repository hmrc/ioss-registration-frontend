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

package services

import connectors.RegistrationConnector
import models.amend.PreviousRegistration
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountService @Inject()(
                                registrationConnector: RegistrationConnector
                              )(implicit ec: ExecutionContext) {

  def getLatestAccount()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    registrationConnector.getAccounts().map { accounts =>
      accounts.enrolments
        .filter(_.activationDate.isDefined)
        .maxBy(_.activationDate.get)
        .identifiers
        .find(_.key == "IOSSNumber")
        .map(_.value)
    }
  }

  def getPreviousRegistrations()(implicit hc: HeaderCarrier): Future[Seq[PreviousRegistration]] = {
    registrationConnector.getAccounts().map { accounts =>
      val accountDetails: Seq[(LocalDate, String)] = accounts
        .enrolments.map(e => e.activationDate -> e.identifiers.find(_.key == "IOSSNumber").map(_.value))
        .collect {
          case (Some(activationDate), Some(iossNumber)) => LocalDate.from(activationDate) -> iossNumber
        }.sortBy(_._1)

      accountDetails.zip(accountDetails.drop(1)).map { case ((activationDate, iossNumber), (nextActivationDate, _)) =>
        PreviousRegistration(
          startPeriod = activationDate,
          endPeriod = nextActivationDate.minusMonths(1),
          iossNumber = iossNumber
        )
      }
    }
  }
}