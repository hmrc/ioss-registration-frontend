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
import models.{Country, Index, PreviousSchemeType}
import models.domain.PreviousSchemeNumbers
import org.scalatest.freespec.AnyFreeSpec
import pages.CheckYourAnswersPage
import pages.euDetails.TaxRegisteredInEuPage
import pages.previousRegistrations._
import queries.previousRegistration.{PreviousRegistrationQuery, PreviousSchemeForCountryQuery}


class PreviousRegistrationsJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val index: Index = Index(0)
  private val index1: Index = Index(1)
  private val country1: Country = Country("AT", "Austria")
  private val country2: Country = Country("HR", "Croatia")
  private val ossScheme = PreviousSchemeType.OSS
  private val schemeNumber = PreviousSchemeNumbers("123", None)
  private val iossScheme = PreviousSchemeType.IOSS
  private val isSameCountry: Boolean = true

  "previously registered for IOSS scheme" - {

    "users who have NOT previously registered for IOSS must go to next page" in {

      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, false),
          pageMustBe(TaxRegisteredInEuPage)
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
          submitAnswer(PreviousEuCountryPage(index), country1),
          pageMustBe(PreviousSchemePage(index, index))
        )
    }

    "previously registered for OSS scheme in an EU country" - {
      "user has registered for OSS Scheme" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), ossScheme),
            pageMustBe(PreviousOssNumberPage(index, index))
          )
      }

      "user must enter scheme number for that EU Country" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), ossScheme),
            submitAnswer(PreviousOssNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(index))
          )
      }
    }

    "previously registered for IOSS scheme in an EU country" - {
      "user has used intermediary" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
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
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), true),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(index))
          )

      }

      "user has NOT used intermediary" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
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
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            pageMustBe(CheckPreviousSchemeAnswersPage(index))
          )
      }

    }

    "must be able to add another existing scheme for that same EU country" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), true),
          pageMustBe(PreviousSchemePage(index, index1)),
          goTo(PreviousSchemeTypePage(index, index1)),
          submitAnswer(PreviousSchemeTypePage(index, index1), ossScheme),
          submitAnswer(PreviousOssNumberPage(index, index1), schemeNumber),
          pageMustBe(CheckPreviousSchemeAnswersPage(index)),
          answersMustContain(PreviousSchemeTypePage(index, index)),
          answersMustContain(PreviousSchemeTypePage(index, index1))
        )
    }

    "must be able to remove existing scheme for that EU country" - {
      "remove if multiple schemes" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            submitAnswer(CheckPreviousSchemeAnswersPage(index), true),
            goTo(PreviousSchemeTypePage(index, index1)),
            submitAnswer(PreviousSchemeTypePage(index, index1), ossScheme),
            submitAnswer(PreviousOssNumberPage(index, index1), schemeNumber),
            goTo(DeletePreviousSchemePage(index, index)),
            removeAddToListItem(PreviousSchemeTypePage(index, index)),
            pageMustBe(AddPreviousRegistrationPage()),
            answersMustNotContain(PreviousSchemeForCountryQuery(index, index))
          )
      }

      "remove if only one scheme registered for that EU country" in {
        startingFrom(PreviouslyRegisteredPage)
          .run(
            submitAnswer(PreviouslyRegisteredPage, true),
            submitAnswer(PreviousEuCountryPage(index), country1),
            goTo(PreviousSchemeTypePage(index, index)),
            submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
            submitAnswer(PreviousIossSchemePage(index, index), false),
            submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
            goTo(DeletePreviousSchemePage(index, index)),
            removeAddToListItem(PreviousSchemeForCountryQuery(index, index)),
            pageMustBe(PreviouslyRegisteredPage),
            answersMustNotContain(PreviousSchemeForCountryQuery(index, index))
          )
      }
    }


    "must be able to see registrations for all registered countries" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
          pageMustBe(AddPreviousRegistrationPage())

        )
    }

    "must be able to add registration for a different EU country" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
          submitAnswer(AddPreviousRegistrationPage(), true),
          pageMustBe(PreviousEuCountryPage(index1))
        )
    }


    "must be able to change added registration details" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
          goTo(CheckPreviousSchemeAnswersPage(index))
        )


    }

    "must be able to remove existing registrations" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
          goTo(DeletePreviousRegistrationPage(index)),
          removeAddToListItem(CheckPreviousSchemeAnswersPage(index)),
          pageMustBe(AddPreviousRegistrationPage()),
          answersMustNotContain(CheckPreviousSchemeAnswersPage(index))
        )

    }

    "must be able to continue the registration journey when finished adding previous registration schemes" in {
      startingFrom(PreviouslyRegisteredPage)
        .run(
          submitAnswer(PreviouslyRegisteredPage, true),
          submitAnswer(PreviousEuCountryPage(index), country1),
          goTo(PreviousSchemeTypePage(index, index)),
          submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
          submitAnswer(PreviousIossSchemePage(index, index), false),
          submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
          submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
          submitAnswer(AddPreviousRegistrationPage(), false),
          pageMustBe(TaxRegisteredInEuPage)
        )
    }

    "must be able to remove all original previous registration answers" - {
      "when the user is on the Check Your Answers page and they change their original answer of having a Previous registration to No" in {

        val initialise = journeyOf(
          setUserAnswerTo(PreviouslyRegisteredPage, true),
          setUserAnswerTo(PreviousEuCountryPage(index), country1),
          setUserAnswerTo(PreviousSchemeTypePage(index, index), iossScheme),
          setUserAnswerTo(PreviousIossSchemePage(index, index), false),
          setUserAnswerTo(PreviousIossNumberPage(index, index), schemeNumber),
          goTo(CheckYourAnswersPage)
        )

        startingFrom(CheckYourAnswersPage)
          .run(
            initialise,
            goToChangeAnswer(PreviouslyRegisteredPage),
            submitAnswer(PreviouslyRegisteredPage, false),
            pageMustBe(DeleteAllPreviousRegistrationsPage),
            submitAnswer(DeleteAllPreviousRegistrationsPage, true),
            removeAddToListItem(PreviousRegistrationQuery(index)),
            answersMustNotContain(PreviousSchemeTypePage(index, index))
          )
      }
    }

    "must be able to retain all original previous registration answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of having a previous registration to No" - {

        "but answer no when asked if they want to remove all previous registration from the scheme" in {

          val initialise = journeyOf(
            setUserAnswerTo(PreviouslyRegisteredPage, true),
            setUserAnswerTo(PreviousEuCountryPage(index), country1),
            setUserAnswerTo(PreviousSchemeTypePage(index, index), iossScheme),
            setUserAnswerTo(PreviousIossSchemePage(index, index), false),
            goTo(CheckYourAnswersPage)
          )

          startingFrom(CheckYourAnswersPage)
            .run(
              initialise,
              goToChangeAnswer(PreviouslyRegisteredPage),
              submitAnswer(PreviouslyRegisteredPage, false),
              pageMustBe(DeleteAllPreviousRegistrationsPage),
              submitAnswer(DeleteAllPreviousRegistrationsPage, false),
              pageMustBe(CheckYourAnswersPage),
              answersMustContain(PreviousSchemeTypePage(index, index))
            )
        }
      }
    }

    "must navigate to correct page when removing previous registrations" - {
      "when the user removes all schemes and has no registrations for any other countries" - {
        "must navigate to PreviouslyRegisteredPage" in {
          startingFrom(PreviouslyRegisteredPage)
            .run(
              submitAnswer(PreviouslyRegisteredPage, true),
              submitAnswer(PreviousEuCountryPage(index), country1),
              goTo(PreviousSchemeTypePage(index, index)),
              submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
              submitAnswer(PreviousIossSchemePage(index, index), false),
              submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
              pageMustBe(CheckPreviousSchemeAnswersPage(index)),
              answersMustContain(PreviousSchemeTypePage(index, index)),
              goTo(DeletePreviousSchemePage(index, index)),
              removeAddToListItem(PreviousSchemeForCountryQuery(index, index)),
              pageMustBe(PreviouslyRegisteredPage),
              answersMustNotContain(PreviousSchemeForCountryQuery(index, index))
            )
        }
      }

      "when the user removes all the schemes for one country but has a previous registration for another country" - {
        "must navigate back to AddPreviousRegistrationPage" in {
          startingFrom(PreviouslyRegisteredPage)
            .run(
              submitAnswer(PreviouslyRegisteredPage, true),
              submitAnswer(PreviousEuCountryPage(index), country1),
              goTo(PreviousSchemeTypePage(index, index)),
              submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
              submitAnswer(PreviousIossSchemePage(index, index), false),
              submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
              submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
              submitAnswer(AddPreviousRegistrationPage(), true),
              submitAnswer(PreviousEuCountryPage(index1), country2),
              goTo(PreviousSchemeTypePage(index1, index)),
              submitAnswer(PreviousSchemeTypePage(index1, index), iossScheme),
              submitAnswer(PreviousIossSchemePage(index1, index), false),
              submitAnswer(PreviousIossNumberPage(index1, index), schemeNumber),
              submitAnswer(CheckPreviousSchemeAnswersPage(index1), false),
              goToChangeAnswer(CheckPreviousSchemeAnswersPage(index)),
              goTo(DeletePreviousSchemePage(index, index)),
              removeAddToListItem(PreviousSchemeForCountryQuery(index, index)),
              pageMustBe(AddPreviousRegistrationPage()),
              answersMustNotContain(PreviousSchemeForCountryQuery(index, index))
            )
        }
      }

      "when the user removes one scheme from a previously registered country but has another scheme for the same country" - {
        "must navigate back to CheckPreviousSchemeAnswersPage" in {
          startingFrom(PreviouslyRegisteredPage)
            .run(
              submitAnswer(PreviouslyRegisteredPage, true),
              submitAnswer(PreviousEuCountryPage(index), country1),
              goTo(PreviousSchemeTypePage(index, index)),
              submitAnswer(PreviousSchemeTypePage(index, index), iossScheme),
              submitAnswer(PreviousIossSchemePage(index, index), false),
              submitAnswer(PreviousIossNumberPage(index, index), schemeNumber),
              submitAnswer(CheckPreviousSchemeAnswersPage(index), true),
              goTo(PreviousSchemeTypePage(index, index1)),
              submitAnswer(PreviousSchemeTypePage(index, index1), ossScheme),
              submitAnswer(PreviousOssNumberPage(index, index1), schemeNumber),
              submitAnswer(CheckPreviousSchemeAnswersPage(index), false),
              goToChangeAnswer(CheckPreviousSchemeAnswersPage(index)),
              isSameCountry match {
                case true =>
                goTo(DeletePreviousSchemePage(index, index))
                removeAddToListItem(PreviousSchemeForCountryQuery(index, index))
                pageMustBe(CheckPreviousSchemeAnswersPage(index))
                answersMustNotContain(PreviousSchemeForCountryQuery(index, index))
              }
            )

        }
      }
    }

  }
}