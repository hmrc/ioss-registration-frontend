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

  /*def incompleteCheckEuDetailsRedirect(mode: Mode)
                                      (implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    val isPartOfVatGroup = request.userAnswers.vatInfo.exists(_.partOfVatGroup)
    firstIndexedIncompleteEuDetails(getAllIncompleteEuDetails().map(_.euCountry)) match {
      case Some(incompleteCountry) =>
        request.userAnswers.get(SellsGoodsToEUConsumersPage(Index(incompleteCountry._2))) match {
          case Some(true) =>
            request.userAnswers.get(SellsGoodsToEUConsumerMethodPage(Index(incompleteCountry._2))) match {
              case Some(EuConsumerSalesMethod.FixedEstablishment) =>
                fixedEstablishmentRedirect(mode, isPartOfVatGroup, incompleteCountry)
              case Some(EuConsumerSalesMethod.DispatchWarehouse) =>
                dispatchWarehouseRedirect(mode, incompleteCountry)
              case None =>
                Some(Redirect(controllers.euDetails.routes.SellsGoodsToEUConsumerMethodController.onPageLoad(mode, Index(incompleteCountry._2))))
            }
          case Some(false) =>
            notSellingGoodsRedirect(mode, incompleteCountry)
          case None =>
            Some(Redirect(controllers.euDetails.routes.SellsGoodsToEUConsumersController.onPageLoad(mode, Index(incompleteCountry._2))))
        }
      case None => None
    }
  }*/

  /*private def fixedEstablishmentRedirect(
                                          mode: Mode,
                                          isPartOfVatGroup: Boolean,
                                          incompleteCountry: (EuOptionalDetails, Int)
                                        )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (isPartOfVatGroup) {
      Some(Redirect(controllers.euDetails.routes.CannotAddCountryController.onPageLoad(mode, Index(incompleteCountry._2))))
    } else {
      request.userAnswers.get(RegistrationTypePage(Index(incompleteCountry._2))) match {
        case Some(RegistrationType.VatNumber) =>
          request.userAnswers.get(EuVatNumberPage(Index(incompleteCountry._2))) match {
            case Some(_) =>
              fixedEstablishingTradeDetailsRedirect(mode, incompleteCountry)
            case None =>
              Some(Redirect(controllers.euDetails.routes.EuVatNumberController.onPageLoad(mode, Index(incompleteCountry._2))))
          }
        case Some(RegistrationType.TaxId) =>
          request.userAnswers.get(EuTaxReferencePage(Index(incompleteCountry._2))) match {
            case Some(_) =>
              fixedEstablishingTradeDetailsRedirect(mode, incompleteCountry)
            case None =>
              Some(Redirect(controllers.euDetails.routes.EuTaxReferenceController.onPageLoad(mode, Index(incompleteCountry._2))))
          }
        case None =>
          Some(Redirect(controllers.euDetails.routes.RegistrationTypeController.onPageLoad(mode, Index(incompleteCountry._2))))
      }
    }
  }*/

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

  /*private def dispatchWarehouseRedirect(
                                         waypoints: Waypoints,
                                         incompleteCountry: (EuOptionalDetails, Int)
                                       )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    request.userAnswers.get(RegistrationTypePage(Index(incompleteCountry._2))) match {
      case Some(RegistrationType.VatNumber) =>
        request.userAnswers.get(EuVatNumberPage(Index(incompleteCountry._2))) match {
          case Some(_) =>
            dispatchWarehouseTradeDetailsRedirect(waypoints, incompleteCountry)
          case None =>
            Some(Redirect(controllers.euDetails.routes.EuVatNumberController.onPageLoad(waypoints, Index(incompleteCountry._2))))
        }
      case Some(RegistrationType.TaxId) =>
        request.userAnswers.get(EuTaxReferencePage(Index(incompleteCountry._2))) match {
          case Some(_) =>
            dispatchWarehouseTradeDetailsRedirect(mode, incompleteCountry)
          case None =>
            Some(Redirect(controllers.euDetails.routes.EuTaxReferenceController.onPageLoad(mode, Index(incompleteCountry._2))))
        }
      case None =>
        Some(Redirect(controllers.euDetails.routes.RegistrationTypeController.onPageLoad(mode, Index(incompleteCountry._2))))
    }
  }

  private def dispatchWarehouseTradeDetailsRedirect(
                                                     waypoints: Waypoints,
                                                     incompleteCountry: (EuOptionalDetails, Int)
                                                   )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    request.userAnswers.get(EuSendGoodsTradingNamePage(Index(incompleteCountry._2))) match {
      case Some(_) =>
        request.userAnswers.get(EuSendGoodsAddressPage(Index(incompleteCountry._2))) match {
          case Some(_) =>
            Some(Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(mode, Index(incompleteCountry._2))))
          case None =>
            Some(Redirect(controllers.euDetails.routes.EuSendGoodsAddressController.onPageLoad(mode, Index(incompleteCountry._2))))
        }
      case None =>
        Some(Redirect(controllers.euDetails.routes.EuSendGoodsTradingNameController.onPageLoad(mode, Index(incompleteCountry._2))))
    }
  }

  private def notSellingGoodsRedirect(
                                       waypoints: Waypoints,
                                       incompleteCountry: (EuOptionalDetails, Int)
                                     )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    request.userAnswers.get(VatRegisteredPage(Index(incompleteCountry._2))) match {
      case Some(true) =>
        request.userAnswers.get(EuVatNumberPage(Index(incompleteCountry._2))) match {
          case Some(_) =>
            Some(Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(mode, Index(incompleteCountry._2))))
          case None =>
            Some(Redirect(controllers.euDetails.routes.EuVatNumberController.onPageLoad(mode, Index(incompleteCountry._2))))
        }
      case Some(false) =>
        Some(Redirect(controllers.euDetails.routes.CheckEuDetailsAnswersController.onPageLoad(mode, Index(incompleteCountry._2))))
      case None =>
        Some(Redirect(controllers.euDetails.routes.VatRegisteredController.onPageLoad(mode, Index(incompleteCountry._2))))
    }
  }*/

}

