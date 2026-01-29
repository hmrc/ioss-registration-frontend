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

import formats.Format
import logging.Logging
import models.core.Match
import models.requests.AuthenticatedDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.{Clock, LocalDate}
import scala.util.Try
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckOtherCountryRegistrationFilterImpl @Inject()(
                                                         registrationModificationMode: RegistrationModificationMode,
                                                         service: CoreRegistrationValidationService,
                                                         clock: Clock
                                                       )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  private val exclusionStatusCode = 4

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def getEffectiveDate(activeMatch: Match): LocalDate = {
      activeMatch.exclusionEffectiveDate.flatMap { dateString =>
        Try(LocalDate.parse(dateString, Format.eisDateFormatter)).toOption
      } match {
        case Some(date) => date
        case _ =>
          val e = new IllegalStateException(s"Exclusion status code ${activeMatch.exclusionStatusCode} didn't include an expected exclusion effective date")
          logger.error(s"Must have an Exclusion Effective Date ${e.getMessage}", e)
          throw e
      }
    }

    service.searchUkVrn(request.vrn)(hc, request).map {
      case _ if registrationModificationMode == AmendingActiveRegistration =>
        None
      case Some(activeMatch)
        if activeMatch.isActiveTrader =>
        Some(Redirect(controllers.filters.routes.AlreadyRegisteredOtherCountryController.onPageLoad(activeMatch.memberState)))

      case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
        Some(Redirect(
          controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            activeMatch.memberState,
            activeMatch.getEffectiveDate)
        ))

      case _ =>
        None
    }
  }
}

class CheckOtherCountryRegistrationFilter @Inject()(
                                                     service: CoreRegistrationValidationService,
                                                     clock: Clock
                                                   )(implicit val executionContext: ExecutionContext) {
  def apply(registrationModificationMode: RegistrationModificationMode): CheckOtherCountryRegistrationFilterImpl = {
    new CheckOtherCountryRegistrationFilterImpl(registrationModificationMode, service, clock)
  }
}