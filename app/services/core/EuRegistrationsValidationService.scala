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

package services.core

import models.PreviousScheme
import models.core.Match
import models.etmp.SchemeType.{OSSNonUnion, OSSUnion}
import models.etmp.{EtmpDisplayEuRegistrationDetails, EtmpPreviousEuRegistrationDetails}
import models.requests.AuthenticatedDataRequest
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait PreviousValidationInvalidResult

case class InvalidActiveTrader(countryCode: String, memberState: String) extends PreviousValidationInvalidResult

case object InvalidQuarantinedTrader extends PreviousValidationInvalidResult

class EuRegistrationsValidationService @Inject()(
                                                  coreRegistrationValidationService: CoreRegistrationValidationService,
                                                  clock: Clock
                                                ) {

  def validateEuRegistrationDetails(euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails])
                                   (implicit hc: HeaderCarrier,
                                    request: AuthenticatedDataRequest[_],
                                    ec: ExecutionContext): Future[Either[PreviousValidationInvalidResult, Boolean]] = {
    euRegistrationDetails.toList match {
      case ::(currentDetails, remaining) =>
        lookupSingleEtmpDisplayEuRegistrationDetails(currentDetails, currentDetails.vatNumber).flatMap { (maybeMatch: Option[Match]) =>
          maybeMatch match {
            case Some(foundMatch) =>
              remapMatchToError(currentDetails.issuedBy, foundMatch) match {
                case Some(previousValidationInvalidResult) => Future.successful(Left(previousValidationInvalidResult))
                case _ => validateEuRegistrationDetails(remaining)
              }
            case _ =>
              validateEuRegistrationDetails(remaining)
          }
        }
      case Nil =>
        Future.successful(Right(true))
    }
  }

  private def remapMatchToError(countryCode: String, foundMatch: Match, isOss: Boolean = false): Option[PreviousValidationInvalidResult] = {
    if (foundMatch.isActiveTrader && !isOss) {
      Some(InvalidActiveTrader(countryCode = countryCode, memberState = foundMatch.memberState))
    }
    else if (foundMatch.isQuarantinedTrader(clock)) {
      Some(InvalidQuarantinedTrader)
    } else {
      None
    }
  }

  private def lookupSingleEtmpDisplayEuRegistrationDetails(details: EtmpDisplayEuRegistrationDetails,
                                                           vatNumber: Option[String])
                                                          (implicit hc: HeaderCarrier,
                                                           request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {
    vatNumber match {
      case Some(euVrn) =>
        coreRegistrationValidationService.searchEuVrn(euVrn, details.issuedBy)
      case _ => details.taxIdentificationNumber match {
        case Some(taxId) =>
          coreRegistrationValidationService.searchEuTaxId(taxId, details.issuedBy)

        case _ => Future.failed(
          new RuntimeException(s"$details has neither a vrn or taxIdentificationNumber")
        )
      }
    }
  }

  def validatePreviousEuRegistrationDetails(previousEURegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails])
                                           (implicit hc: HeaderCarrier,
                                            request: AuthenticatedDataRequest[_],
                                            ec: ExecutionContext): Future[Either[PreviousValidationInvalidResult, Boolean]] = {

    previousEURegistrationDetails.toList match {
      case ::(currentPreviousEuRegistrationDetails, remaining) =>
        validateSingle(currentPreviousEuRegistrationDetails, remaining)

      case Nil =>
        Future.successful(Right(true))
    }
  }

  private def validateSingle(currentPreviousEuRegistrationDetails: EtmpPreviousEuRegistrationDetails,
                             next: List[EtmpPreviousEuRegistrationDetails])
                            (implicit hc: HeaderCarrier,
                             request: AuthenticatedDataRequest[_],
                             ec: ExecutionContext): Future[Either[PreviousValidationInvalidResult, Boolean]] = {

    val ossSchemaTypes = List(OSSNonUnion, OSSUnion)
    val isOss = ossSchemaTypes.contains(currentPreviousEuRegistrationDetails.schemeType)

    coreRegistrationValidationService.searchScheme(
      currentPreviousEuRegistrationDetails.registrationNumber,
      PreviousScheme.fromEmtpSchemeType(currentPreviousEuRegistrationDetails.schemeType),
      currentPreviousEuRegistrationDetails.intermediaryNumber,
      currentPreviousEuRegistrationDetails.issuedBy
    ).flatMap {
      case Some(foundMatch) =>
        remapMatchToError(currentPreviousEuRegistrationDetails.issuedBy, foundMatch, isOss) match {
          case Some(previousValidationInvalidResult) =>
            Future.successful(Left(previousValidationInvalidResult))
          case None =>
            validatePreviousEuRegistrationDetails(next)
        }

      case _ =>
        validatePreviousEuRegistrationDetails(next)
    }
  }
}
