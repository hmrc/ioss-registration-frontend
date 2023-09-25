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

import models._
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.BusinessContactDetailsPage
import pages.checkVatDetails.CheckVatDetailsPage
import pages.euDetails._
import pages.previousRegistrations._
import pages.tradingNames.{AddTradingNamePage, DeleteAllTradingNamesPage, TradingNamePage}
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryFixedEstablishmentTradingNameUserAnswersEntry: Arbitrary[(FixedEstablishmentTradingNamePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[FixedEstablishmentTradingNamePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryEuTaxReferenceUserAnswersEntry: Arbitrary[(EuTaxReferencePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[EuTaxReferencePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryEuVatNumberUserAnswersEntry: Arbitrary[(EuVatNumberPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[EuVatNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryRegistrationTypeUserAnswersEntry: Arbitrary[(RegistrationTypePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[RegistrationTypePage]
        value <- arbitrary[RegistrationType].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySellsGoodsToEuConsumerMethodUserAnswersEntry: Arbitrary[(SellsGoodsToEuConsumerMethodPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[SellsGoodsToEuConsumerMethodPage]
        value <- arbitrary[EuConsumerSalesMethod].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryEuCountryUserAnswersEntry: Arbitrary[(EuCountryPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[EuCountryPage]
        value <- arbitrary[Country].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryTaxRegisteredInEuUserAnswersEntry: Arbitrary[(TaxRegisteredInEuPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[TaxRegisteredInEuPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDeleteAllTradingNamesUserAnswersEntry: Arbitrary[(DeleteAllTradingNamesPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllTradingNamesPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAddTradingNameUserAnswersEntry: Arbitrary[(AddTradingNamePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[AddTradingNamePage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryTradingNameUserAnswersEntry: Arbitrary[(TradingNamePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[TradingNamePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCheckVatDetailsUserAnswersEntry: Arbitrary[(CheckVatDetailsPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[CheckVatDetailsPage.type]
        value <- arbitrary[CheckVatDetails].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryBusinessContactDetailsUserAnswersEntry: Arbitrary[(BusinessContactDetailsPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[BusinessContactDetailsPage.type]
        value <- arbitrary[BusinessContactDetails].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDeleteAllPreviousRegistrationsUserAnswersEntry: Arbitrary[(DeleteAllPreviousRegistrationsPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllPreviousRegistrationsPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDeletePreviousSchemeUserAnswersEntry: Arbitrary[(DeletePreviousSchemePage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[DeletePreviousSchemePage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousIossNumberUserAnswersEntry: Arbitrary[(PreviousSchemeNumbersPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousSchemeNumbersPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousIossSchemeUserAnswersEntry: Arbitrary[(PreviousIossSchemePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousIossSchemePage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousSchemePageUserAnswersEntry: Arbitrary[(PreviousSchemePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousSchemePage]
        value <- arbitrary[PreviousScheme].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousSchemeTypePageUserAnswersEntry: Arbitrary[(PreviousSchemeTypePage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousSchemeTypePage]
        value <- arbitrary[PreviousSchemeType].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviouslyRegisteredUserAnswersEntry: Arbitrary[(PreviouslyRegisteredPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviouslyRegisteredPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousEuVatNumberUserAnswersEntry: Arbitrary[(PreviousOssNumberPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousOssNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPreviousEuCountryUserAnswersEntry: Arbitrary[(PreviousEuCountryPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[PreviousEuCountryPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAddPreviousRegistrationUserAnswersEntry: Arbitrary[(AddPreviousRegistrationPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[AddPreviousRegistrationPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
}
