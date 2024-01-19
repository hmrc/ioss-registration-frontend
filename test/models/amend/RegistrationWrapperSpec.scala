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

package models.amend

import base.SpecBase
import models.DesAddress
import models.domain.VatCustomerInfo
import models.etmp._
import play.api.libs.json.{JsSuccess, Json}
import testutils.RegistrationData.etmpDisplayRegistration

class RegistrationWrapperSpec extends SpecBase {

  private val desAddress: DesAddress = arbitraryDesAddress.arbitrary.sample.value

  private val vatInfo: VatCustomerInfo = vatCustomerInfo.copy(desAddress = desAddress)

  private val tradingNames: Seq[EtmpTradingName] = etmpDisplayRegistration.tradingNames
  private val schemeDetails: EtmpDisplaySchemeDetails = etmpDisplayRegistration.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpDisplayRegistration.bankDetails
  private val exclusions: Seq[EtmpExclusion] = etmpDisplayRegistration.exclusions
  private val adminUse: EtmpAdminUse = etmpDisplayRegistration.adminUse

  "RegistrationWrapper" - {

    "must serialise/deserialise to and from RegistrationWrapper" in {

      val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatInfo, etmpDisplayRegistration)

      val expectedJson = Json.obj(
        "vatInfo" -> Json.obj(
          "desAddress" -> vatInfo.desAddress,
          "partOfVatGroup" -> vatInfo.partOfVatGroup,
          "registrationDate" -> vatInfo.registrationDate,
          "organisationName" -> vatInfo.organisationName,
          "singleMarketIndicator" -> vatInfo.singleMarketIndicator,
          "overseasIndicator" -> vatInfo.overseasIndicator
        ),
        "registration" -> Json.obj(
          "tradingNames" -> tradingNames,
          "schemeDetails" -> schemeDetails,
          "bankDetails" -> bankDetails,
          "exclusions" -> exclusions,
          "adminUse" -> adminUse
        )
      )

      Json.toJson(registrationWrapper) mustBe expectedJson
      expectedJson.validate[RegistrationWrapper] mustBe JsSuccess(registrationWrapper)
    }
  }
}
