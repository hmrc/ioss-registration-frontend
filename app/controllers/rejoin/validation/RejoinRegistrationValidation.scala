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

package controllers.rejoin.validation

import controllers.euDetails.routes.{ExcludedVRNController, FixedEstablishmentVRNAlreadyRegisteredController}
import controllers.previousRegistrations.routes.{SchemeQuarantinedController, SchemeStillActiveController}
import logging.Logging
import models.amend.RegistrationWrapper
import models.etmp.EtmpDisplaySchemeDetails
import models.requests.AuthenticatedDataRequest
import pages.{EmptyWaypoints, Waypoints}
import play.api.mvc.Call
import services.core.{EuRegistrationsValidationService, InvalidActiveTrader, InvalidQuarantinedTrader, PreviousValidationInvalidResult}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RejoinRegistrationValidation @Inject()(euRegistrationsValidationService: EuRegistrationsValidationService) extends Logging {


  def validateEuRegistrations(registrationWrapper: RegistrationWrapper, waypoints: Waypoints)(implicit hc: HeaderCarrier,
                                                                                              request: AuthenticatedDataRequest[_],
                                                                                              ec: ExecutionContext): Future[Either[Call, Boolean]] = {

    val schemeDetails = registrationWrapper.registration.schemeDetails
    euRegistrationsValidationService.validateEuRegistrationDetails(schemeDetails.euRegistrationDetails)
      .flatMap {
        case Left(previousValidationInvalidResult) =>
          remapInvalidEuRegistrationResultToRedirect(waypoints, schemeDetails, previousValidationInvalidResult)
        case Right(_) =>
          findInfractionInPreviousEuRegistration(schemeDetails)
      }
  }



  private def remapInvalidEuRegistrationResultToRedirect(waypoints: Waypoints,
                                                         schemeDetails: EtmpDisplaySchemeDetails,
                                                         previousValidationInvalidResult: PreviousValidationInvalidResult): Future[Left[Call, Nothing]] = {
    previousValidationInvalidResult match {
      case invalidActiveTraderResult: InvalidActiveTrader =>
        logger.info(
          s"EuRegistration ${schemeDetails.previousEURegistrationDetails} has been mapped to InvalidActiveTraderResult"
        )
        Future.successful(
          Left(
            FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(
              waypoints,
              invalidActiveTraderResult.countryCode
            )))
      case InvalidQuarantinedTrader =>
        logger.info(
          s"EuRegistration ${schemeDetails.previousEURegistrationDetails} has been mapped to InvalidQuarantinedTraderResult"
        )
        Future.successful(Left(ExcludedVRNController.onPageLoad()))
    }
  }


  private def findInfractionInPreviousEuRegistration(schemeDetails: EtmpDisplaySchemeDetails)(implicit hc: HeaderCarrier,
                                                                                              request: AuthenticatedDataRequest[_],
                                                                                              ec: ExecutionContext): Future[Either[Call, Boolean]] = {
    euRegistrationsValidationService.validatePreviousEuRegistrationDetails(schemeDetails.previousEURegistrationDetails)
      .flatMap {
        case Left(previousValidationInvalidResult) =>
          previousValidationInvalidResult match {
            case InvalidActiveTrader(countryCode, _) =>
              logger.info(
                s"PreviousEuRegistration ${schemeDetails.previousEURegistrationDetails} has been mapped to InvalidActiveTraderResult"
              )
              Future.successful(Left(SchemeStillActiveController.onPageLoad(EmptyWaypoints, countryCode)))

            case InvalidQuarantinedTrader =>
              logger.info(
                s"PreviousEuRegistration ${schemeDetails.previousEURegistrationDetails} has been mapped to InvalidQuarantinedTraderResult"
              )
              Future.successful(Left(SchemeQuarantinedController.onPageLoad()))
          }

        case Right(_) => Future.successful(Right(true))
      }
  }
}
