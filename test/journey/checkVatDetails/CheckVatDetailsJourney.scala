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

package journey.checkVatDetails

import base.SpecBase
import generators.ModelGenerators
import journey.JourneyHelpers
import models.CheckVatDetails
import org.scalatest.freespec.AnyFreeSpec
import pages.checkVatDetails.{CheckVatDetailsPage, UpdateVatDetailsPage, UseOtherAccountPage}
import pages.tradingNames.HasTradingNamePage

class CheckVatDetailsJourney extends AnyFreeSpec with JourneyHelpers with ModelGenerators with SpecBase {

  "users who have confirmed their VAT details can carry on to register for IOSS" in {

    startingFrom(CheckVatDetailsPage)
      .run(
        setUserAnswerTo(basicUserAnswersWithVatInfo),
        submitAnswer(CheckVatDetailsPage, CheckVatDetails.Yes),
        pageMustBe(HasTradingNamePage)
      )
  }

  "users who have identified their VAT details as being incorrect can update their VAT details" in {

    startingFrom(CheckVatDetailsPage)
      .run(
        setUserAnswerTo(basicUserAnswersWithVatInfo),
        submitAnswer(CheckVatDetailsPage, CheckVatDetails.DetailsIncorrect),
        pageMustBe(UpdateVatDetailsPage)
      )
  }

  "users who have identified their VAT details as being the wrong account can use another account" in {

    startingFrom(CheckVatDetailsPage)
      .run(
        setUserAnswerTo(basicUserAnswersWithVatInfo),
        submitAnswer(CheckVatDetailsPage, CheckVatDetails.WrongAccount),
        pageMustBe(UseOtherAccountPage)
      )
  }
}
