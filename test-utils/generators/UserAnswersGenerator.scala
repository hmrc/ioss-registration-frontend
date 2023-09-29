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

import models.UserAnswers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages.{QuestionPage, _}
import pages.checkVatDetails.CheckVatDetailsPage
import pages.euDetails._
import pages.tradingNames.{AddTradingNamePage, DeleteAllTradingNamesPage, TradingNamePage}
import pages.previousRegistrations._
import play.api.libs.json.{JsValue, Json}

trait UserAnswersGenerator extends TryValues {
  self: Generators =>

  val generators: Seq[Gen[(QuestionPage[_], JsValue)]] = {
      arbitrary[(DeleteAllEuDetailsPage.type, JsValue)] ::
      arbitrary[(AddEuDetailsPage, JsValue)] ::
      arbitrary[(FixedEstablishmentAddressPage, JsValue)] ::
      arbitrary[(FixedEstablishmentTradingNamePage, JsValue)] ::
      arbitrary[(EuTaxReferencePage, JsValue)] ::
      arbitrary[(EuVatNumberPage, JsValue)] ::
      arbitrary[(RegistrationTypePage, JsValue)] ::
      arbitrary[(SellsGoodsToEuConsumerMethodPage, JsValue)] ::
      arbitrary[(EuCountryPage, JsValue)] ::
      arbitrary[(TaxRegisteredInEuPage.type, JsValue)] ::
      arbitrary[(DeleteAllTradingNamesPage.type, JsValue)] ::
      arbitrary[(AddTradingNamePage, JsValue)] ::
      arbitrary[(TradingNamePage, JsValue)] ::
      arbitrary[(CheckVatDetailsPage.type, JsValue)] ::
      arbitrary[(BusinessContactDetailsPage.type, JsValue)] ::
      arbitrary[(DeleteAllPreviousRegistrationsPage.type, JsValue)] ::
      arbitrary[(PreviousSchemePage, JsValue)] ::
      arbitrary[(PreviousSchemeTypePage, JsValue)] ::
      arbitrary[(PreviousSchemeNumbersPage, JsValue)] ::
      arbitrary[(PreviousIossSchemePage, JsValue)] ::
      arbitrary[(PreviouslyRegisteredPage.type, JsValue)] ::
      arbitrary[(PreviousOssNumberPage, JsValue)] ::
      arbitrary[(PreviousEuCountryPage, JsValue)] ::
      arbitrary[(PreviousEuCountryPage, JsValue)] ::
      Nil
  }

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] = {

    import models._

    Arbitrary {
      for {
        id <- nonEmptyString
        data <- generators match {
          case Nil => Gen.const(Map[QuestionPage[_], JsValue]())
          case _ => Gen.mapOf(oneOf(generators))
        }
      } yield UserAnswers(
        id = id,
        data = data.foldLeft(Json.obj()) {
          case (obj, (path, value)) =>
            obj.setObject(path.path, value).get
        }
      )
    }
  }
}
