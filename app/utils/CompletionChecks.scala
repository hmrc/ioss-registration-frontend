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

package utils

import models._
import models.euDetails.EuOptionalDetails
import models.previousRegistrations.PreviousRegistrationDetailsWithOptionalVatNumber
import models.requests.AuthenticatedDataRequest
import pages._
import pages.previousRegistrations.{PreviousSchemeTypePage, PreviouslyRegisteredPage}
import pages.tradingNames._
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.AllWebsites
import queries.euDetails.AllEuOptionalDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsWithOptionalVatNumberQuery
import queries.tradingNames.AllTradingNames
import utils.EuDetailsCompletionChecks._

import scala.concurrent.Future

trait CompletionChecks {


  protected def withCompleteDataModel[A](index: Index, data: Index => Option[A], onFailure: Option[A] => Result)
                                        (onSuccess: => Result): Result = {
    val incomplete = data(index)
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {

    val incomplete = data()
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  def getAllIncompleteDeregisteredDetails()(implicit request: AuthenticatedDataRequest[AnyContent]): Seq[PreviousRegistrationDetailsWithOptionalVatNumber] = {
    request.userAnswers
      .get(AllPreviousRegistrationsWithOptionalVatNumberQuery).map(
      _.filter(scheme =>
        scheme.previousSchemesDetails.isEmpty || scheme.previousSchemesDetails.getOrElse(List.empty).exists(_.previousSchemeNumbers.isEmpty))
    ).getOrElse(List.empty)
  }

  def firstIndexedIncompleteDeregisteredCountry(incompleteCountries: Seq[Country])
                                               (implicit request: AuthenticatedDataRequest[AnyContent]):
  Option[(PreviousRegistrationDetailsWithOptionalVatNumber, Int)] = {
    request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.previousEuCountry))
  }

  def firstIndexedIncompleteEuDetails(incompleteCountries: Seq[Country])
                                     (implicit request: AuthenticatedDataRequest[AnyContent]): Option[(EuOptionalDetails, Int)] = {
    request.userAnswers.get(AllEuOptionalDetailsQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.euCountry))
  }

  private def isTradingNamesValid()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(HasTradingNamePage).exists {
      case true => request.userAnswers.get(AllTradingNames).getOrElse(List.empty).nonEmpty
      case false => request.userAnswers.get(AllTradingNames).getOrElse(List.empty).isEmpty
    }
  }

  private def hasWebsiteValid()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(AllWebsites).getOrElse(List.empty).nonEmpty
  }

  private def isDeregisteredPopulated()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(PreviouslyRegisteredPage).exists {
      case true => request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).isDefined
      case false => request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).getOrElse(List.empty).isEmpty
    }
  }

  def validate()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    getAllIncompleteDeregisteredDetails().isEmpty &&
      getAllIncompleteEuDetails().isEmpty &&
      isTradingNamesValid() &&
      hasWebsiteValid() &&
      isEuDetailsPopulated() &&
      isDeregisteredPopulated()
  }

  def getFirstValidationErrorRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    (incompleteTradingNameRedirect(waypoints) ++
      emptyEuDetailsRedirect(waypoints) ++
      incompleteCheckEuDetailsRedirect(waypoints) ++
      emptyDeregisteredRedirect(waypoints) ++
      incompletePreviousRegistrationRedirect(waypoints)
      ).headOption
  }

  def incompletePreviousRegistrationRedirect(waypoints: Waypoints)
                                            (implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] =
    firstIndexedIncompleteDeregisteredCountry(getAllIncompleteDeregisteredDetails().map(_.previousEuCountry)) match {
      case Some(incompleteCountry) if incompleteCountry._1.previousSchemesDetails.isDefined =>
        incompleteCountry._1.previousSchemesDetails.getOrElse(List.empty).zipWithIndex.find(_._1.previousSchemeNumbers.isEmpty) match {
          case Some(schemeDetails) =>
            request.userAnswers.get(PreviousSchemeTypePage(Index(incompleteCountry._2), Index(schemeDetails._2))) match {
              case Some(PreviousSchemeType.OSS) =>
                Some(Redirect(controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(
                  waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
              case Some(PreviousSchemeType.IOSS) =>
                schemeDetails._1.previousScheme match {
                  case Some(_) =>
                    Some(Redirect(controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(
                      waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
                  case None =>
                    Some(Redirect(controllers.previousRegistrations.routes.PreviousIossSchemeController.onPageLoad(
                      waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
                }
              case None => None
            }
          case None => None
        }

      case Some(incompleteCountry) =>
        Some(Redirect(controllers.previousRegistrations.routes.PreviousSchemeController.onPageLoad(
          waypoints, Index(incompleteCountry._2), Index(incompleteCountry._2))))

      case None => None
    }

  private def incompleteTradingNameRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = if (!isTradingNamesValid()) {
    Some(Redirect(controllers.tradingNames.routes.HasTradingNameController.onPageLoad(waypoints)))
  } else {
    None
  }

  private def emptyDeregisteredRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = if (!isDeregisteredPopulated()) {
    Some(Redirect(controllers.previousRegistrations.routes.PreviouslyRegisteredController.onPageLoad(waypoints)))
  } else {
    None
  }
}
