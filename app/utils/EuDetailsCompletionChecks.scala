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

import models.euDetails.{EuConsumerSalesMethod, EuOptionalDetails, RegistrationType}
import models.requests.AuthenticatedDataRequest
import models.{CountryWithValidationDetails, Index}
import pages.Waypoints
import pages.euDetails._
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.EuOptionalDetailsQuery
import queries.euDetails.AllEuOptionalDetailsQuery

case object EuDetailsCompletionChecks extends CompletionChecks {

  def isEuDetailsPopulated()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(TaxRegisteredInEuPage).exists {
      case true => request.userAnswers.get(AllEuOptionalDetailsQuery).isDefined
      case false => request.userAnswers.get(AllEuOptionalDetailsQuery).getOrElse(List.empty).isEmpty
    }
  }

  def emptyEuDetailsRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = if (!isEuDetailsPopulated) {
    Some(Redirect(controllers.euDetails.routes.TaxRegisteredInEuController.onPageLoad(waypoints)))
  } else {
    None
  }

  def getIncompleteEuDetails(index: Index)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[EuOptionalDetails] = {
    val isPartOfVatGroup = request.userAnswers.vatInfo.exists(_.partOfVatGroup)
    request.userAnswers
      .get(EuOptionalDetailsQuery(index))
      .find { details =>
        sellsGoodsToEuConsumersMethod(isPartOfVatGroup, details) || checkVatNumber(details)
      }
  }

  def getAllIncompleteEuDetails()(implicit request: AuthenticatedDataRequest[AnyContent]): Seq[EuOptionalDetails] = {
    val isPartOfVatGroup = request.userAnswers.vatInfo.exists(_.partOfVatGroup)
    request.userAnswers
      .get(AllEuOptionalDetailsQuery).map(
      _.filter { details =>
        sellsGoodsToEuConsumersMethod(isPartOfVatGroup, details) || checkVatNumber(details)
      }
    ).getOrElse(List.empty)
  }

  private def checkVatNumber(details: EuOptionalDetails): Boolean = {
    details.euVatNumber.exists { euVatNumber =>
      CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == details.euCountry.code) match {
        case Some(validationRule) =>
          !euVatNumber.matches(validationRule.vrnRegex)
        case _ => true
      }
    }
  }

  private def sellsGoodsToEuConsumersMethod(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    details.sellsGoodsToEuConsumerMethod.isEmpty ||
      details.sellsGoodsToEuConsumerMethod.contains(EuConsumerSalesMethod.FixedEstablishment) && details.registrationType.isEmpty ||
      (details.registrationType.contains(RegistrationType.VatNumber) && details.euVatNumber.isEmpty) ||
      (details.registrationType.contains(RegistrationType.TaxId) && details.euTaxReference.isEmpty) ||
      fixedEstablishment(isPartOfVatGroup, details)
  }

  private def fixedEstablishment(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    !isPartOfVatGroup && details.sellsGoodsToEuConsumerMethod.contains(EuConsumerSalesMethod.FixedEstablishment) &&
      (details.registrationType.contains(RegistrationType.TaxId) || details.registrationType.contains(RegistrationType.VatNumber)) &&
      (details.fixedEstablishmentTradingName.isEmpty || details.fixedEstablishmentAddress.isEmpty) /*||
      (isPartOfVatGroup && details.sellsGoodsToEuConsumerMethod.contains(EuConsumerSalesMethod.FixedEstablishment))*/
  }

  def incompleteEuDetailsRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    firstIndexedIncompleteEuDetails(getAllIncompleteEuDetails().map(
      _.euCountry
    )).map(
      incompleteCountry =>
        Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, Index(incompleteCountry._2)))
    )
  }
}

