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

import models.Index
import models.euDetails.{EuConsumerSalesMethod, EuOptionalDetails, RegistrationType}
import models.requests.AuthenticatedDataRequest
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
      .find(details =>
        partOfVatGroup(isPartOfVatGroup, details) || notPartOfVatGroup(isPartOfVatGroup, details))
  }

  def getAllIncompleteEuDetails()(implicit request: AuthenticatedDataRequest[AnyContent]): Seq[EuOptionalDetails] = {
    val isPartOfVatGroup = request.userAnswers.vatInfo.exists(_.partOfVatGroup)
    request.userAnswers
      .get(AllEuOptionalDetailsQuery).map(
      _.filter(details =>
        partOfVatGroup(isPartOfVatGroup, details) ||
          notPartOfVatGroup(isPartOfVatGroup, details)
      )
    ).getOrElse(List.empty)
  }

  private def partOfVatGroup(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    isPartOfVatGroup && notSellingToEuConsumers(details) || sellsToEuConsumers(isPartOfVatGroup, details)
  }

  private def notPartOfVatGroup(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    !isPartOfVatGroup && notSellingToEuConsumers(details) || sellsToEuConsumers(isPartOfVatGroup, details)
  }

  private def notSellingToEuConsumers(details: EuOptionalDetails): Boolean = {
    details.sellsGoodsToEUConsumers.isEmpty ||
      (details.sellsGoodsToEUConsumers.contains(false) && !details.vatRegistered.contains(true)) ||
      (details.vatRegistered.contains(true) && details.euVatNumber.isEmpty)
  }

  private def sellsToEuConsumers(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    (details.sellsGoodsToEUConsumers.contains(true) && details.sellsGoodsToEUConsumerMethod.isEmpty) ||
      (details.sellsGoodsToEUConsumerMethod.contains(EuConsumerSalesMethod.DispatchWarehouse) && details.registrationType.isEmpty) ||
      (details.registrationType.contains(RegistrationType.VatNumber) && details.euVatNumber.isEmpty) ||
      (details.registrationType.contains(RegistrationType.TaxId) && details.euTaxReference.isEmpty) ||
      fixedEstablishment(isPartOfVatGroup, details) || sendsGoods(details)
  }

  private def sendsGoods(details: EuOptionalDetails): Boolean = {
    (details.sellsGoodsToEUConsumerMethod.contains(EuConsumerSalesMethod.DispatchWarehouse) &&
      (details.registrationType.contains(RegistrationType.TaxId) || details.registrationType.contains(RegistrationType.VatNumber)) &&
      (details.euSendGoodsTradingName.isEmpty || details.euSendGoodsAddress.isEmpty))
  }

  private def fixedEstablishment(isPartOfVatGroup: Boolean, details: EuOptionalDetails): Boolean = {
    (!isPartOfVatGroup && details.sellsGoodsToEUConsumerMethod.contains(EuConsumerSalesMethod.FixedEstablishment) &&
      (details.registrationType.contains(RegistrationType.TaxId) || details.registrationType.contains(RegistrationType.VatNumber)) &&
      (details.fixedEstablishmentTradingName.isEmpty || details.fixedEstablishmentAddress.isEmpty)) ||
      (isPartOfVatGroup && details.sellsGoodsToEUConsumerMethod.contains(EuConsumerSalesMethod.FixedEstablishment))
  }

  def incompleteEuDetailsRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] =
    firstIndexedIncompleteEuDetails(getAllIncompleteEuDetails().map(
      _.euCountry
    )).map(
      incompleteCountry =>
        Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, Index(incompleteCountry._2)))
    )

    private def fixedEstablishmentRedirect(
                                          waypoints: Waypoints,
                                          isPartOfVatGroup: Boolean,
                                          incompleteCountry: (EuOptionalDetails, Int)
                                        )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (isPartOfVatGroup) {
      Some(Redirect(controllers.euDetails.routes.CannotAddCountryController.onPageLoad(waypoints, Index(incompleteCountry._2))))
    } else {
      request.userAnswers.get(RegistrationTypePage(Index(incompleteCountry._2))) match {
        case Some(RegistrationType.VatNumber) =>
          request.userAnswers.get(EuVatNumberPage(Index(incompleteCountry._2))) match {
            case Some(_) =>
              fixedEstablishingTradeDetailsRedirect(waypoints, incompleteCountry)
            case None =>
              Some(Redirect(controllers.euDetails.routes.EuVatNumberController.onPageLoad(waypoints, Index(incompleteCountry._2))))
          }
        case Some(RegistrationType.TaxId) =>
          request.userAnswers.get(EuTaxReferencePage(Index(incompleteCountry._2))) match {
            case Some(_) =>
              fixedEstablishingTradeDetailsRedirect(waypoints, incompleteCountry)
            case None =>
              Some(Redirect(controllers.euDetails.routes.EuTaxReferenceController.onPageLoad(waypoints, Index(incompleteCountry._2))))
          }
        case None =>
          Some(Redirect(controllers.euDetails.routes.RegistrationTypeController.onPageLoad(waypoints, Index(incompleteCountry._2))))
      }
    }
  }
  private def fixedEstablishingTradeDetailsRedirect(
                                                     waypoints: Waypoints,
                                                     incompleteCountry: (EuOptionalDetails, Int)
                                                   )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    request.userAnswers.get(FixedEstablishmentTradingNamePage(Index(incompleteCountry._2))) match {
      case Some(_) =>
        request.userAnswers.get(FixedEstablishmentAddressPage(Index(incompleteCountry._2))) match {
          case Some(_) =>
            Some(Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, Index(incompleteCountry._2))))
          case None =>
            Some(Redirect(controllers.euDetails.routes.FixedEstablishmentAddressController.onPageLoad(waypoints, Index(incompleteCountry._2))))
        }
      case None =>
        Some(Redirect(controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(waypoints, Index(incompleteCountry._2))))
    }
  }
}

