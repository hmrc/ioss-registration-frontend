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

package generators

import models.Index
import org.scalacheck.Arbitrary
import pages._
import pages.checkVatDetails.CheckVatDetailsPage
import pages.euDetails.{EuCountryPage, RegistrationTypePage, SellsGoodsToEuConsumerMethodPage, TaxRegisteredInEuPage}
import pages.tradingNames.{AddTradingNamePage, DeleteAllTradingNamesPage, TradingNamePage}
import pages.previousRegistrations._

trait PageGenerators {

  implicit lazy val arbitraryRegistrationTypePage: Arbitrary[RegistrationTypePage] =
    Arbitrary(RegistrationTypePage(Index(0)))

  implicit lazy val arbitrarySellsGoodsToEuConsumerMethodPage: Arbitrary[SellsGoodsToEuConsumerMethodPage] =
    Arbitrary(SellsGoodsToEuConsumerMethodPage(Index(0)))

  implicit lazy val arbitraryEuCountryPage: Arbitrary[EuCountryPage] =
    Arbitrary(EuCountryPage(Index(0)))

  implicit lazy val arbitraryTaxRegisteredInEuPage: Arbitrary[TaxRegisteredInEuPage.type] =
    Arbitrary(TaxRegisteredInEuPage)

  implicit lazy val arbitraryDeleteAllTradingNamesPage: Arbitrary[DeleteAllTradingNamesPage.type] =
    Arbitrary(DeleteAllTradingNamesPage)

  implicit lazy val arbitraryAddTradingNamePage: Arbitrary[AddTradingNamePage] =
    Arbitrary(AddTradingNamePage(Some(Index(1))))

  implicit lazy val arbitraryTradingNamePage: Arbitrary[TradingNamePage] =
    Arbitrary(TradingNamePage(Index(0)))

  implicit lazy val arbitraryCheckVatDetailsPage: Arbitrary[CheckVatDetailsPage.type] =
    Arbitrary(CheckVatDetailsPage)

  implicit lazy val arbitraryBusinessContactDetailsPage: Arbitrary[BusinessContactDetailsPage.type] =
    Arbitrary(BusinessContactDetailsPage)

  implicit lazy val arbitraryDeleteAllPreviousRegistrationsPage: Arbitrary[DeleteAllPreviousRegistrationsPage.type] =
    Arbitrary(DeleteAllPreviousRegistrationsPage)

  implicit lazy val arbitraryAddPreviousRegistrationPage: Arbitrary[AddPreviousRegistrationPage.type] =
    Arbitrary(AddPreviousRegistrationPage)

  implicit lazy val arbitraryDeletePreviousSchemePage: Arbitrary[DeletePreviousSchemePage.type] =
    Arbitrary(DeletePreviousSchemePage)

  implicit lazy val arbitraryPreviousEuCountryPage: Arbitrary[PreviousEuCountryPage] =
    Arbitrary(PreviousEuCountryPage(Index(0)))

  implicit lazy val arbitraryPreviousIossSchemePage: Arbitrary[PreviousIossSchemePage] =
    Arbitrary(PreviousIossSchemePage(Index(0), Index(0)))

  implicit lazy val arbitraryPreviouslyRegisteredPage: Arbitrary[PreviouslyRegisteredPage.type] =
    Arbitrary(PreviouslyRegisteredPage)

  implicit lazy val arbitraryPreviousEuVatNumberPage: Arbitrary[PreviousOssNumberPage] =
    Arbitrary(PreviousOssNumberPage(Index(0), Index(0)))

  implicit lazy val arbitraryPreviousIossNumberPage: Arbitrary[PreviousSchemeNumbersPage] =
    Arbitrary(PreviousIossNumberPage(Index(0), Index(0)))

  implicit lazy val arbitraryPreviousSchemePage: Arbitrary[PreviousSchemePage] =
    Arbitrary(PreviousSchemePage(Index(0), Index(0)))

  implicit lazy val arbitraryPreviousSchemeTypePage: Arbitrary[PreviousSchemeTypePage] =
    Arbitrary(PreviousSchemeTypePage(Index(0), Index(0)))

}
