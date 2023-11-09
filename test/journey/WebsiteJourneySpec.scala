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
import models.{BusinessContactDetails, CheckMode, Index, Website}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import pages.amend.ChangeRegistrationPage
import pages.website.{AddWebsitePage, DeleteWebsitePage, WebsitePage}
import pages.{BusinessContactDetailsPage, NonEmptyWaypoints, Waypoint, Waypoints}

class WebsiteJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val maxWebsites = 10
  private val index: Index = Index(0)
  private val firstPage = WebsitePage(Index(0))

  private val amendWaypoints: Waypoints =
    NonEmptyWaypoints(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment), List.empty[Waypoint])

  private def createWebSiteSubmission(index: Int, submitAnother: Boolean): JourneyStep[Unit] = {
    journeyOf(
      submitAnswer(WebsitePage(Index(index)), Website(s"www.website-$index.com")),
      submitAnswer(AddWebsitePage(), submitAnother),
    )
  }

  private def generateWebsites(count: Int): Seq[JourneyStep[Unit]] = {
    (0 until count).map(index =>
      createWebSiteSubmission(index, submitAnother = true))
  }

  private def createChangeAnswerSubmission(index: Int): JourneyStep[Unit] = {
    journeyOf(
      goToChangeAnswer(WebsitePage(Index(index))),
      submitAnswer(WebsitePage(Index(index)), Website(s"updated-www.website-$index.com")),
    )
  }

  private def setWebsites(count: Int): Seq[JourneyStep[Unit]] = {
    (0 until count).map { index =>
      setUserAnswerTo(WebsitePage(Index(index)), Website(s"www.website-$index.com"))
    }
  }

  "users with one or more websites" - {

    "can add websites up to the max number of websites" in {
      val calls = generateWebsites(maxWebsites) :+
        pageMustBe(BusinessContactDetailsPage)

      startingFrom(firstPage).run(calls: _*)
    }

    "must be able to remove them" - {

      "when there is only one" in {
        startingFrom(firstPage)
          .run(
            createWebSiteSubmission(0, submitAnother = false),
            goTo(DeleteWebsitePage(index)),
            removeAddToListItem(AddWebsitePage(Some(index))),
            pageMustBe(AddWebsitePage())
          )
      }

      "when there are multiple" in {

        val calls = generateWebsites(count = 4) ++ List(
          goTo(DeleteWebsitePage(Index(2))),
          removeAddToListItem(WebsitePage(Index(2))),
          pageMustBe(AddWebsitePage()),
          answersMustContain(WebsitePage(index)),
          answersMustContain(WebsitePage(Index(1))),
          answersMustContain(WebsitePage(Index(2))),
          answerMustEqual(WebsitePage(Index(2)), Website("www.website-3.com")),
          answersMustNotContain(WebsitePage(Index(3)))
        )

        startingFrom(firstPage)
          .run(calls: _*)
      }
    }

    "must be able to change the users original answer" - {

      "when there is only one" in {
        startingFrom(firstPage)
          .run(
            createWebSiteSubmission(0, submitAnother = false),
            goTo(AddWebsitePage()),
            createChangeAnswerSubmission(0),
            pageMustBe(AddWebsitePage()),
            answerMustEqual(WebsitePage(Index(0)), Website("updated-www.website-0.com"))
          )
      }
    }

    "when there are multiple changes required" in {
      val initialise = journeyOf {
        generateWebsites(4) :+ goTo(AddWebsitePage()): _*
      }

      startingFrom(firstPage)
        .run(
          initialise,
          createChangeAnswerSubmission(1),
          createChangeAnswerSubmission(2),
          pageMustBe(AddWebsitePage()),
          answerMustEqual(WebsitePage(Index(0)), Website("www.website-0.com")),
          answerMustEqual(WebsitePage(Index(1)), Website("updated-www.website-1.com")),
          answerMustEqual(WebsitePage(Index(2)), Website("updated-www.website-2.com")),
          answerMustEqual(WebsitePage(Index(3)), Website("www.website-3.com"))
        )
    }

    "when inAmend" - {

      val initialise = journeyOf {
        setWebsites(5) :+
          setUserAnswerTo(BusinessContactDetailsPage, arbitrary[BusinessContactDetails].sample.value) :+
          goTo(ChangeRegistrationPage): _*
      }

      "must be able to change existing answers" in {

        startingFrom(ChangeRegistrationPage, amendWaypoints)
          .run(
            initialise,
            createChangeAnswerSubmission(0),
            createChangeAnswerSubmission(3),
            pageMustBe(AddWebsitePage()),
            submitAnswer(AddWebsitePage(), false),
            pageMustBe(ChangeRegistrationPage),
            answerMustEqual(WebsitePage(Index(0)), Website("updated-www.website-0.com")),
            answerMustEqual(WebsitePage(Index(1)), Website("www.website-1.com")),
            answerMustEqual(WebsitePage(Index(2)), Website("www.website-2.com")),
            answerMustEqual(WebsitePage(Index(3)), Website("updated-www.website-3.com")),
            answerMustEqual(WebsitePage(Index(4)), Website("www.website-4.com"))
          )
      }

      "must be able to add new websites" in {

        startingFrom(ChangeRegistrationPage, amendWaypoints)
          .run(
            initialise,
            goTo(AddWebsitePage()),
            submitAnswer(AddWebsitePage(), true),
            createWebSiteSubmission(5, submitAnother = false),
            pageMustBe(ChangeRegistrationPage),
            answerMustEqual(WebsitePage(Index(0)), Website("www.website-0.com")),
            answerMustEqual(WebsitePage(Index(1)), Website("www.website-1.com")),
            answerMustEqual(WebsitePage(Index(2)), Website("www.website-2.com")),
            answerMustEqual(WebsitePage(Index(3)), Website("www.website-3.com")),
            answerMustEqual(WebsitePage(Index(4)), Website("www.website-4.com")),
            answerMustEqual(WebsitePage(Index(5)), Website("www.website-5.com"))
          )
      }

      "must be able to remove websites" in {

        startingFrom(ChangeRegistrationPage, amendWaypoints)
          .run(
            initialise,
            goTo(AddWebsitePage()),
            goTo(DeleteWebsitePage(Index(2))),
            removeAddToListItem(AddWebsitePage(Some(Index(2)))),
            pageMustBe(AddWebsitePage()),
            submitAnswer(AddWebsitePage(), false),
            pageMustBe(ChangeRegistrationPage),
            answerMustEqual(WebsitePage(Index(0)), Website("www.website-0.com")),
            answerMustEqual(WebsitePage(Index(1)), Website("www.website-1.com")),
            answerMustEqual(WebsitePage(Index(2)), Website("www.website-2.com")),
            answerMustEqual(WebsitePage(Index(3)), Website("www.website-3.com")),
            answerMustEqual(WebsitePage(Index(4)), Website("www.website-4.com")),
            answersMustNotContain(WebsitePage(Index(5)))
          )
      }
    }
  }
}
