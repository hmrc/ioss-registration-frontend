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

package journey.tradingNames

import config.Constants.maxTradingNames
import generators.ModelGenerators
import journey.JourneyHelpers
import models.{Index, TradingName}
import org.scalatest.freespec.AnyFreeSpec
import pages.CheckYourAnswersPage
import pages.euDetails.TaxRegisteredInEuPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames._
import queries.tradingNames.AllTradingNames

class TradingNameJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val companyNameA: TradingName = arbitraryTradingName.arbitrary.sample.value
  private val companyNameB: TradingName = arbitraryTradingName.arbitrary.sample.value
  private val companyNameC: TradingName = arbitraryTradingName.arbitrary.sample.value

  "users with one or more trading names" - {

    s"must be asked for as many as necessary upto the maximum of $maxTradingNames" in {

      startingFrom(HasTradingNamePage)
        .run(
          submitAnswer(HasTradingNamePage, true),
          submitAnswer(TradingNamePage(Index(0)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(0))), true),
          submitAnswer(TradingNamePage(Index(1)), companyNameB),
          submitAnswer(AddTradingNamePage(Some(Index(1))), true),
          submitAnswer(TradingNamePage(Index(2)), companyNameC),
          submitAnswer(AddTradingNamePage(Some(Index(2))), true),
          submitAnswer(TradingNamePage(Index(3)), companyNameB),
          submitAnswer(AddTradingNamePage(Some(Index(3))), true),
          submitAnswer(TradingNamePage(Index(4)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(4))), true),
          submitAnswer(TradingNamePage(Index(5)), companyNameC),
          submitAnswer(AddTradingNamePage(Some(Index(5))), true),
          submitAnswer(TradingNamePage(Index(6)), companyNameC),
          submitAnswer(AddTradingNamePage(Some(Index(6))), true),
          submitAnswer(TradingNamePage(Index(7)), companyNameB),
          submitAnswer(AddTradingNamePage(Some(Index(7))), true),
          submitAnswer(TradingNamePage(Index(8)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(8))), true),
          submitAnswer(TradingNamePage(Index(9)), companyNameB),
          submitAnswer(AddTradingNamePage(Some(Index(9))), false),
          pageMustBe(PreviouslyRegisteredPage)
        )
    }

    "must be able to remove them" - {

      "when there is only one" in {

        startingFrom(HasTradingNamePage)
          .run(
            submitAnswer(HasTradingNamePage, true),
            submitAnswer(TradingNamePage(Index(0)), companyNameA),
            submitAnswer(AddTradingNamePage(Some(Index(0))), false),
            goTo(DeleteTradingNamePage(Index(0))),
            removeAddToListItem(TradingNamePage(Index(0))),
            pageMustBe(HasTradingNamePage),
            answersMustNotContain(TradingNamePage(Index(0)))
          )
      }

      "when there are multiple" in {

        startingFrom(HasTradingNamePage)
          .run(
            submitAnswer(HasTradingNamePage, true),
            submitAnswer(TradingNamePage(Index(0)), companyNameA),
            submitAnswer(AddTradingNamePage(Some(Index(0))), true),
            submitAnswer(TradingNamePage(Index(1)), companyNameB),
            submitAnswer(AddTradingNamePage(Some(Index(1))), true),
            submitAnswer(TradingNamePage(Index(2)), companyNameC),
            submitAnswer(AddTradingNamePage(Some(Index(2))), false),
            goTo(DeleteTradingNamePage(Index(1))),
            removeAddToListItem(TradingNamePage(Index(1))),
            pageMustBe(AddTradingNamePage()),
            answersMustNotContain(TradingNamePage(Index(2)))
          )
      }
    }

    "must be able to change the users original answer" - {

      "when there is only one" in {

        val initialise = journeyOf(
          submitAnswer(HasTradingNamePage, true),
          submitAnswer(TradingNamePage(Index(0)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(0))), false),
          goTo(AddTradingNamePage())
        )

        startingFrom(HasTradingNamePage)
          .run(
            initialise,
            goToChangeAnswer(TradingNamePage(Index(0))),
            submitAnswer(TradingNamePage(Index(0)), companyNameB),
            pageMustBe(AddTradingNamePage()),
            answerMustEqual(TradingNamePage(Index(0)), companyNameB)
          )
      }

      "when there are multiple changes required" in {

        val initialise = journeyOf(
          submitAnswer(HasTradingNamePage, true),
          submitAnswer(TradingNamePage(Index(0)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(0))), true),
          submitAnswer(TradingNamePage(Index(1)), companyNameB),
          submitAnswer(AddTradingNamePage(Some(Index(1))), true),
          submitAnswer(TradingNamePage(Index(2)), companyNameC),
          submitAnswer(AddTradingNamePage(Some(Index(2))), true),
          submitAnswer(TradingNamePage(Index(3)), companyNameA),
          submitAnswer(AddTradingNamePage(Some(Index(3))), false),
          goTo(AddTradingNamePage())
        )

        startingFrom(HasTradingNamePage)
          .run(
            initialise,
            goToChangeAnswer(TradingNamePage(Index(0))),
            submitAnswer(TradingNamePage(Index(0)), companyNameB),
            pageMustBe(AddTradingNamePage()),
            goToChangeAnswer(TradingNamePage(Index(2))),
            submitAnswer(TradingNamePage(Index(2)), companyNameC),
            pageMustBe(AddTradingNamePage()),
            answerMustEqual(TradingNamePage(Index(0)), companyNameB),
            answerMustEqual(TradingNamePage(Index(2)), companyNameC)
          )
      }
    }

    "must be able to remove all original trading name answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of having a different UK trading name to No" in {

        val initialise = journeyOf(
          setUserAnswerTo(HasTradingNamePage, true),
          setUserAnswerTo(TradingNamePage(Index(0)), companyNameA),
          setUserAnswerTo(AddTradingNamePage(Some(Index(0))), true),
          setUserAnswerTo(TradingNamePage(Index(1)), companyNameB),
          setUserAnswerTo(AddTradingNamePage(Some(Index(1))), true),
          setUserAnswerTo(TradingNamePage(Index(2)), companyNameC),
          setUserAnswerTo(AddTradingNamePage(Some(Index(2))), true),
          setUserAnswerTo(TradingNamePage(Index(3)), companyNameA),
          setUserAnswerTo(AddTradingNamePage(Some(Index(3))), false),
          goTo(CheckYourAnswersPage)
        )

        startingFrom(CheckYourAnswersPage)
          .run(
            initialise,
            goToChangeAnswer(HasTradingNamePage),
            submitAnswer(HasTradingNamePage, false),
            pageMustBe(DeleteAllTradingNamesPage),
            submitAnswer(DeleteAllTradingNamesPage, true),
            removeAddToListItem(AllTradingNames()),
            pageMustBe(CheckYourAnswersPage),
            answersMustNotContain(TradingNamePage(Index(0))),
            answersMustNotContain(TradingNamePage(Index(1))),
            answersMustNotContain(TradingNamePage(Index(2))),
            answersMustNotContain(TradingNamePage(Index(3)))
          )
      }
    }

    "must be able to retain all original trading name answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of having a different UK trading name to No" - {

        "but answer no when asked if they want to remove all trading names from the scheme" in {

          val initialise = journeyOf(
            setUserAnswerTo(HasTradingNamePage, true),
            setUserAnswerTo(TradingNamePage(Index(0)), companyNameA),
            setUserAnswerTo(AddTradingNamePage(Some(Index(0))), true),
            setUserAnswerTo(TradingNamePage(Index(1)), companyNameB),
            setUserAnswerTo(AddTradingNamePage(Some(Index(1))), true),
            setUserAnswerTo(TradingNamePage(Index(2)), companyNameC),
            setUserAnswerTo(AddTradingNamePage(Some(Index(2))), true),
            setUserAnswerTo(TradingNamePage(Index(3)), companyNameA),
            setUserAnswerTo(AddTradingNamePage(Some(Index(3))), false),
            goTo(CheckYourAnswersPage)
          )

          startingFrom(CheckYourAnswersPage)
            .run(
              initialise,
              goToChangeAnswer(HasTradingNamePage),
              submitAnswer(HasTradingNamePage, false),
              pageMustBe(DeleteAllTradingNamesPage),
              submitAnswer(DeleteAllTradingNamesPage, false),
              pageMustBe(CheckYourAnswersPage),
              answersMustContain(TradingNamePage(Index(0))),
              answersMustContain(TradingNamePage(Index(1))),
              answersMustContain(TradingNamePage(Index(2))),
              answersMustContain(TradingNamePage(Index(3)))
            )
        }
      }
    }
  }
}
