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

package services.core

import connectors.core.ValidateCoreRegistrationConnector
import logging.Logging
import models.CountryWithValidationDetails.convertTaxIdentifierForTransfer
import models.PreviousScheme
import models.audit.CoreRegistrationAuditModel
import models.core.{CoreRegistrationRequest, Match, SourceType}
import models.requests.AuthenticatedDataRequest
import services.AuditService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreRegistrationValidationService @Inject()(
                                                   connector: ValidateCoreRegistrationConnector,
                                                   auditService: AuditService,
                                                 )
                                                 (implicit ec: ExecutionContext) extends Logging {

  def searchUkVrn(vrn: Vrn)(implicit hc: HeaderCarrier,
                            request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {

    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.VATNumber.toString, None, vrn.vrn, None, "GB")

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchEuTaxId(euTaxReference: String, countryCode: String)(implicit hc: HeaderCarrier,
                                                                 request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {

    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.EUTraderId.toString, None, euTaxReference, None, countryCode)

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchEuVrn(euVrn: String, countryCode: String)(implicit hc: HeaderCarrier,
                                                      request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {

    val convertedEuVrn = convertTaxIdentifierForTransfer(euVrn, countryCode)
    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.EUTraderId.toString, None, convertedEuVrn, None, countryCode)

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchScheme(searchNumber: String, previousScheme: PreviousScheme, intermediaryNumber: Option[String], countryCode: String)
                  (implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {

    if (previousScheme == PreviousScheme.OSSNU) {
      Future.successful(None)
    } else {

      val sourceType = previousScheme match {
        case PreviousScheme.OSSU => SourceType.EUTraderId
        case PreviousScheme.OSSNU => SourceType.EUTraderId
        case PreviousScheme.IOSSWOI => SourceType.TraderId
        case PreviousScheme.IOSSWI => SourceType.TraderId
      }

      val convertedSearchNumber = if (sourceType == SourceType.EUTraderId) {
        convertTaxIdentifierForTransfer(searchNumber, countryCode)
      } else {
        searchNumber
      }

      val coreRegistrationRequest = CoreRegistrationRequest(
        source = sourceType.toString,
        scheme = Some(convertScheme(previousScheme)),
        searchId = convertedSearchNumber,
        searchIntermediary = intermediaryNumber,
        searchIdIssuedBy = countryCode
      )

      getValidateCoreRegistrationResponse(coreRegistrationRequest)
    }
  }

  private def getValidateCoreRegistrationResponse(coreRegistrationRequest: CoreRegistrationRequest)
                                                 (implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {
    connector.validateCoreRegistration(coreRegistrationRequest).map {
      case Right(coreRegistrationResponse) =>
        auditService.audit(CoreRegistrationAuditModel.build(coreRegistrationRequest, coreRegistrationResponse))
        coreRegistrationResponse.matches.headOption
      case Left(errorResponse) =>
        logger.error(s"failed getting registration response $errorResponse")
        throw CoreRegistrationValidationException("Error while validating core registration")
    }
  }

  private def convertScheme(previousScheme: PreviousScheme): String = {
    previousScheme match {
      case PreviousScheme.OSSU => "OSS"
      case PreviousScheme.OSSNU => "OSS"
      case PreviousScheme.IOSSWOI => "IOSS"
      case PreviousScheme.IOSSWI => "IOSS"
    }
  }
}

case class CoreRegistrationValidationException(message: String) extends Exception(message)
