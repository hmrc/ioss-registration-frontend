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

package journey.previousRegistrations

import generators.ModelGenerators
import journey.JourneyHelpers
import models.domain.PreviousSchemeNumbers
import models.{Country, Index, PreviousSchemeType}
import org.scalatest.freespec.AnyFreeSpec
import pages.filters.RegisteredForIossInEuPage
import pages.previousRegistrations._


class PreviousRegistrationsJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val index = Index(0)
  private val index1 = Index(1)
  private val country = Country.euCountries.head
  private val ossScheme = PreviousSchemeType.OSS
  private val schemeNumber = PreviousSchemeNumbers("123", None)
  private val iossScheme = PreviousSchemeType.IOSS

  "previously registered for IOSS scheme" - {

    "users who have NOT previously registered for IOSS must go to next page" in {

      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, false),
            pageMustBe(RegisteredForIossInEuPage)//TODO registered-for-vat-in-EU
        )
    }

    "users who have previously registered for IOSS must go to EU Country page" in {

      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          pageMustBe(PreviousEuCountryPage(index))
        )
    }

    "users who have previously registered in EU Country must select scheme type" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          pageMustBe(PreviousSchemePage(index, index))
        )
    }

    "previously registered for OSS scheme in an EU country" - {
      "user has registered for OSS Scheme" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), ossScheme),
            pageMustBe(PreviousOssNumberPage(index, index))
          )
      }

      "user must enter scheme number for that EU Country" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), ossScheme),
            submitAnswer(PreviousOssNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(Some(index)))
          )
      }
    }

    "previously registered for IOSS scheme in an EU country" - {
      "user has used intermediary" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), true),
            pageMustBe(PreviousIossNumberPage(index, index))
          )
      }

      "must add previous IOSS number and Intermediary Identification Number" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), true),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(Some(index)))
          )

      }

      "user has NOT used intermediary" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            pageMustBe(PreviousIossNumberPage(index, index))
          )
      }

      "user must add previous IOSS number" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(Some(index)))
          )
      }

    }

    "must be able to add another existing scheme for that same EU country" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), true),
          pageMustBe(PreviousSchemePage(index, index1))
        )
    }

    "must be able to remove existing scheme for that EU country" - {
      "remove if multiple schemes" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), true),
            goTo(PreviousSchemeTypePage(index, index1)),
            submitAnswer(PreviousSchemeTypePage(index, index1), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index1), false),
            submitAnswer(PreviousIossNumberPage(index, index1), schemeNumber),
            goTo(DeletePreviousSchemePage(index)),
            removeAddToListItem(PreviousSchemeTypePage(index, index)),
            pageMustBe(CheckPreviousSchemeAnswersPage(Some(index))),
            answersMustNotContain(PreviousSchemeTypePage(index, index))
          )
      }

      "remove if only one schemes" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            goTo(DeletePreviousSchemePage(index)),
            removeAddToListItem(PreviousSchemeTypePage(index, index)),
            pageMustBe(CheckPreviousSchemeAnswersPage(Some(index))),
            answersMustNotContain(PreviousSchemeTypePage(index, index))
          )
      }


    }


    "must be able to see registrations for all registered countries" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), false),
          pageMustBe(AddPreviousRegistrationPage())

        )
    }

    "must be able to add registration for a different EU country" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), false),
          submitAnswer(AddPreviousRegistrationPage(), true),
          pageMustBe(PreviousEuCountryPage(index1))
        )
    }


    "must be able to change added registration details" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), false),
          goTo(CheckPreviousSchemeAnswersPage())
        )


    }

    "must be able to remove existing registrations" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), false),
          goTo(DeletePreviousRegistrationPage(index)),
          removeAddToListItem(CheckPreviousSchemeAnswersPage(Some(index))),
          pageMustBe(AddPreviousRegistrationPage()),
          answersMustNotContain(CheckPreviousSchemeAnswersPage(Some(index)))
        )

    }

    "must be able to continue the registration journey when finished adding previous registration schemes" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(Some(index)), false),
          submitAnswer(AddPreviousRegistrationPage(), false),
          pageMustBe(RegisteredForIossInEuPage)
        )
    }

  }
}