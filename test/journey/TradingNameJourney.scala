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

package journey

import config.Constants.maxTradingNames
import generators.ModelGenerators
import models.{Index, TradingName}
import org.scalatest.freespec.AnyFreeSpec
import pages.{AddTradingNamePage, DeleteTradingNamePage, HasTradingNamePage, TradingNamePage}

class TradingNameJourney extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val companyName: TradingName = arbitraryTradingName.arbitrary.sample.value

  "users with one or more trading names" - {

    s"must be asked for as many as necessary upto the maximum of $maxTradingNames" in {

      startingFrom(HasTradingNamePage)
        .run(
          submitAnswer(HasTradingNamePage, true),
          submitAnswer(TradingNamePage(Index(0)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(0))), true),
          submitAnswer(TradingNamePage(Index(1)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(1))), true),
          submitAnswer(TradingNamePage(Index(2)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(2))), true),
          submitAnswer(TradingNamePage(Index(3)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(3))), true),
          submitAnswer(TradingNamePage(Index(4)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(4))), true),
          submitAnswer(TradingNamePage(Index(5)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(5))), true),
          submitAnswer(TradingNamePage(Index(6)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(6))), true),
          submitAnswer(TradingNamePage(Index(7)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(7))), true),
          submitAnswer(TradingNamePage(Index(8)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(8))), true),
          submitAnswer(TradingNamePage(Index(9)), companyName),
          submitAnswer(AddTradingNamePage(Some(Index(9))), false),
          pageMustBe(HasTradingNamePage) // TODO change to prev reg when created
        )
    }

    "must be able to remove them" - {

      "when there is only one" in {

        startingFrom(HasTradingNamePage)
          .run(
            submitAnswer(HasTradingNamePage, true),
            submitAnswer(TradingNamePage(Index(0)), companyName),
            submitAnswer(AddTradingNamePage(Some(Index(0))), false),
            goTo(DeleteTradingNamePage(Index(0))),
            removeAddToListItem(TradingNamePage(Index(0))),
            pageMustBe(HasTradingNamePage)
          )
      }

      "when there are multiple" in {

        startingFrom(HasTradingNamePage)
          .run(
            submitAnswer(HasTradingNamePage, true),
            submitAnswer(TradingNamePage(Index(0)), companyName),
            submitAnswer(AddTradingNamePage(Some(Index(0))), true),
            submitAnswer(TradingNamePage(Index(1)), companyName),
            submitAnswer(AddTradingNamePage(Some(Index(1))), true),
            submitAnswer(TradingNamePage(Index(2)), companyName),
            submitAnswer(AddTradingNamePage(Some(Index(2))), false),
            goTo(DeleteTradingNamePage(Index(1))),
            removeAddToListItem(TradingNamePage(Index(1))),
            pageMustBe(AddTradingNamePage())
          )
      }
    }
  }
}
