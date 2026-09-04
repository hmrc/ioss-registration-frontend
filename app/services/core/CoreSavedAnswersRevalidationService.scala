/*
 * Copyright 2026 HM Revenue & Customs
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

import controllers.routes
import jakarta.inject.Inject
import logging.Logging
import models.PreviousScheme
import models.core.Match
import models.domain.VatCustomerInfo
import models.previousRegistrations.*
import models.requests.AuthenticatedDataRequest
import models.euDetails.EuDetails
import pages.Waypoints
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsWithOptionalVatNumberQuery
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class CoreSavedAnswersRevalidationService @Inject()(
                                                     coreRegistrationValidationService: CoreRegistrationValidationService,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends Logging {

  def checkAndValidateSavedUserAnswers(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    revalidateUKVrn(waypoints, request.vrn).flatMap {
      case Some(result) =>
        Future.successful(Some(result))

      case None =>
        checkEuDetails(waypoints).flatMap {
          case Some(result) =>
            Future.successful(Some(result))

          case None =>
            checkPreviousRegistrations(waypoints)
        }
    }
  }


  private def checkEuDetails(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    val euDetails: List[EuDetails] =
      request.userAnswers
        .get(AllEuDetailsQuery)
        .getOrElse(List.empty)

    checkAllEuDetails(waypoints, euDetails)
  }

  private def checkPreviousRegistrations(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    val previousRegistrations =
      request.userAnswers
        .get(AllPreviousRegistrationsWithOptionalVatNumberQuery)
        .getOrElse(List.empty)

    checkAllPreviousRegistrations(
      waypoints,
      previousRegistrations
    )
  }

  private def revalidateUKVrn(waypoints: Waypoints, ukVrn: Vrn)(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    if (checkVrnExpired(request.userAnswers.vatInfo)) {
      Some(Redirect(routes.ExpiredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints).url)).toFuture
    } else {
      coreRegistrationValidationService.searchUkVrn(ukVrn).flatMap { maybeActiveMatch =>
        activeMatchRedirectUrl(waypoints, maybeActiveMatch)
      }
    }
  }

  private def checkAllEuDetails(
                                 waypoints: Waypoints,
                                 allEuDetails: List[EuDetails]
                               )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    allEuDetails match {
      case ::(currentEuDetails, remaining) =>
        revalidateEuDetails(waypoints, currentEuDetails).flatMap {
          case Some(urlString) => Some(urlString).toFuture
          case _ => checkAllEuDetails(waypoints, remaining)
        }

      case Nil => None.toFuture
    }
  }

  private def revalidateEuDetails(
                                   waypoints: Waypoints,
                                   euDetails: EuDetails
                                 )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    euDetails.euVatNumber match {
      case Some(euVrn) =>
        revalidateEuVrn(waypoints, euVrn, euDetails.euCountry.code)

      case _ => euDetails.euTaxReference match {
        case Some(euTaxReference) =>
          revalidateEuTaxId(waypoints, euTaxReference, euDetails.euCountry.code)

        case _ =>
          None.toFuture
      }
    }
  }

  private def checkAllPreviousRegistrations(
                                             waypoints: Waypoints,
                                             allPreviousRegistrations: List[PreviousRegistrationDetailsWithOptionalVatNumber]
                                           )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    allPreviousRegistrations match {
      case ::(PreviousRegistrationDetailsWithOptionalVatNumber(
        country,
        Some(optionalSchemeDetails)
      ), remaining) =>
        revalidatePreviousSchemeDetails(
          waypoints = waypoints,
          countryCode = country.code,
          allPreviousSchemeDetails = optionalSchemeDetails
        ).flatMap {
          case Some(urlString) =>
            Some(urlString).toFuture

          case _ =>
            checkAllPreviousRegistrations(waypoints, remaining)
        }

      case ::(_, remaining) =>
        checkAllPreviousRegistrations(waypoints, remaining)

      case Nil => None.toFuture
    }
  }

  private def revalidatePreviousSchemeDetails(
                                               waypoints: Waypoints,
                                               countryCode: String,
                                               allPreviousSchemeDetails: List[SchemeDetailsWithOptionalVatNumber]
                                             )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    allPreviousSchemeDetails match {
      case ::(SchemeDetailsWithOptionalVatNumber(Some(previousScheme), Some(previousSchemeNumbers)), remaining) =>

        previousSchemeNumbers.previousSchemeNumber match {
          case Some(previousSchemeNumber) =>
            coreRegistrationValidationService.searchScheme(
              searchNumber = previousSchemeNumber,
              previousScheme = previousScheme,
              intermediaryNumber = previousSchemeNumbers.previousIntermediaryNumber,
              countryCode = countryCode
            ).flatMap { maybeMatch =>

              previousSchemeRedirect(waypoints, previousScheme, maybeMatch) match {
                case Some(result) =>
                  Some(result).toFuture

                case None =>
                  revalidatePreviousSchemeDetails(waypoints, countryCode, remaining)
              }
            }
          case None =>
            revalidatePreviousSchemeDetails(waypoints, countryCode, remaining)
        }
        

      case ::(_, remaining) =>
        revalidatePreviousSchemeDetails(waypoints, countryCode, remaining)

      case Nil => None.toFuture
    }
  }

  private def previousSchemeRedirect(waypoints: Waypoints, previousScheme: PreviousScheme, maybeMatch: Option[Match]): Option[Result] = {

    previousScheme match {

      case PreviousScheme.OSSU =>
        maybeMatch match {

          case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
            Some(Redirect(
              routes.QuarantinedVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints, activeMatch.memberState, activeMatch.getEffectiveDate)
            ))

          case _ =>
            None
        }

      case PreviousScheme.OSSNU =>
        None

      case _ =>
        None
    }
  }

  private def revalidateEuTaxId(
                                 waypoints: Waypoints,
                                 euTaxReference: String,
                                 countryCode: String
                               )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuTaxId(euTaxReference, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(waypoints, maybeActiveMatch)
    }
  }

  private def revalidateEuVrn(
                               waypoints: Waypoints,
                               euVrn: String,
                               countryCode: String
                             )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(waypoints, maybeActiveMatch)
    }
  }

  private def activeMatchRedirectUrl(waypoints: Waypoints, maybeMatch: Option[Match]): Future[Option[Result]] = {
    maybeMatch match {
      case Some(activeMatch) if activeMatch.isActiveTrader =>
        Some(Redirect(controllers.routes.AlreadyRegisteredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints, activeMatch.memberState))).toFuture

      case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
        Some(Redirect(routes.QuarantinedVatCannotBeUsedForSaveAndComeBackController.onPageLoad(
          waypoints, activeMatch.memberState, activeMatch.getEffectiveDate
        ).url)).toFuture

      case _ =>
        None.toFuture
    }
  }

  private def checkVrnExpired(vatCustomerInfo: Option[VatCustomerInfo]): Boolean = {
    vatCustomerInfo match {
      case Some(vatInfo) =>
        vatInfo.deregistrationDecisionDate.exists(!_.isAfter(LocalDate.now(clock)))

      case _ => false
    }
  }
}
