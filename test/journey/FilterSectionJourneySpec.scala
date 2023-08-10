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

import generators.ModelGenerators
import org.scalatest.freespec.AnyFreeSpec
import pages.filters._

class FilterSectionJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  "users who have already for IOSS in EU must go to the Cannot Register Already Registered page" in {

    startingFrom(RegisteredForIossInEuPage)
      .run(
        submitAnswer(RegisteredForIossInEuPage, true),
        pageMustBe(CannotRegisterAlreadyRegisteredPage)
      )
  }

  "users who have not already for IOSS in EU must go to the Sells Goods To Eu Or Ni page" in {

    startingFrom(RegisteredForIossInEuPage)
      .run(
        submitAnswer(RegisteredForIossInEuPage, false),
        pageMustBe(SellsGoodsToEuOrNiPage)
      )
  }

  "users who sell goods outside the single market must go to the Goods Consignment Value page" in {

    startingFrom(SellsGoodsToEuOrNiPage)
      .run(
        submitAnswer(SellsGoodsToEuOrNiPage, true),
        pageMustBe(GoodsConsignmentValuePage)
      )
  }

  "users who do not sell goods outside the single market must go to the Cannot Register For Ioss page" in {

    startingFrom(SellsGoodsToEuOrNiPage)
      .run(
        submitAnswer(SellsGoodsToEuOrNiPage, false),
        pageMustBe(CannotRegisterForIossPage)
      )
  }

  "users who sell goods outside the single market" - {

    "and their goods have a consignment value which doesn't exceed £135 must go to the Registered For Vat In Uk page" in {

      startingFrom(GoodsConsignmentValuePage)
        .run(
          submitAnswer(GoodsConsignmentValuePage, true),
          pageMustBe(RegisteredForVatInUkPage)
        )
    }

    "and their goods have a consignment value which exceeds £135 must go to the Consignment Value Exceeds Limit page" in {

      startingFrom(GoodsConsignmentValuePage)
        .run(
          submitAnswer(GoodsConsignmentValuePage, false),
          pageMustBe(ConsignmentValueExceedsLimitPage)
        )
    }
  }

  "users who's business is registered for VAT in the UK must go to the Business Based In Ni page" in {

    startingFrom(RegisteredForVatInUkPage)
      .run(
        submitAnswer(RegisteredForVatInUkPage, true),
        pageMustBe(BusinessBasedInNiPage)
      )
  }

  "users who's business is not registered for VAT in the UK must go to the Cannot Register No Vat In Uk page" in {

    startingFrom(RegisteredForVatInUkPage)
      .run(
        submitAnswer(RegisteredForVatInUkPage, false),
        pageMustBe(CannotRegisterNoVatInUkPage)
      )
  }

  "users who's business trades under the Northern Ireland protocol must go to the Eligible To Register page" in {

    startingFrom(BusinessBasedInNiPage)
      .run(
        submitAnswer(BusinessBasedInNiPage, true),
        pageMustBe(EligibleToRegisterPage)
      )
  }

  "users who's business does not trade under the Northern Ireland protocol must go to the Norwegian Based Business page" in {

    startingFrom(BusinessBasedInNiPage)
      .run(
        submitAnswer(BusinessBasedInNiPage, false),
        pageMustBe(NorwegianBasedBusinessPage)
      )
  }

  "users who's business is a Norwegian business must go to the Eligible To Register page" in {

    startingFrom(NorwegianBasedBusinessPage)
      .run(
        submitAnswer(NorwegianBasedBusinessPage, true),
        pageMustBe(EligibleToRegisterPage)
      )
  }

  "users who's business is not a Norwegian business must go to the Cannot Register No Ni Or Norway Business page" in {

    startingFrom(NorwegianBasedBusinessPage)
      .run(
        submitAnswer(NorwegianBasedBusinessPage, false),
        pageMustBe(CannotRegisterNoNiOrNorwayBusinessPage)
      )
  }
}
