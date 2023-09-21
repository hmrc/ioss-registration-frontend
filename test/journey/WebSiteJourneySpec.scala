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
import models.{Index, Website}
import org.scalatest.freespec.AnyFreeSpec
import pages.website.{AddWebsitePage, DeleteWebsitePage, WebsitePage}
import pages.BusinessContactDetailsPage

class WebSiteJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators {

  private val firstPage = WebsitePage(Index(0))

  "users with one or more websites" - {
    "can add websites up to the max number of websites" in {
      val calls = generateWebsites(10) :+
        pageMustBe(BusinessContactDetailsPage)

      startingFrom(firstPage).run(calls: _*)
    }

    def createWebSiteSubmission(index: Int, submitAnother: Boolean): JourneyStep[Unit] = {
      journeyOf(
        submitAnswer(WebsitePage(Index(index)), Website(s"website-$index")),
        submitAnswer(AddWebsitePage(Some(Index(index))), submitAnother),
      )
    }

    def generateWebsites(count: Int): Seq[JourneyStep[Unit]] = {
      (0 until count).map(index =>
        createWebSiteSubmission(index, submitAnother = true))

    }


    "must be able to remove them" - {
      "when there is only one" in {
        startingFrom(firstPage)
          .run(
            createWebSiteSubmission(0, submitAnother = false),
            goTo(DeleteWebsitePage(Index(0))),
            removeAddToListItem(AddWebsitePage(Some(Index(0)))),
            pageMustBe(AddWebsitePage())
          )
      }

      "when there are multiple" in {

        val calls = generateWebsites(count = 4) ++ List(
          goTo(DeleteWebsitePage(Index(2))),
          removeAddToListItem(WebsitePage(Index(2))),
          pageMustBe(AddWebsitePage()),
          answersMustContain(WebsitePage(Index(0))),
          answersMustContain(WebsitePage(Index(1))),
          answersMustContain(WebsitePage(Index(2))),
          answerMustEqual(WebsitePage(Index(2)), Website("website-3")),
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
            answerMustEqual(WebsitePage(Index(0)), Website("updated-website-0"))
          )
      }
    }

    def createChangeAnswerSubmission(index: Int): JourneyStep[Unit] = {
      journeyOf(
        goToChangeAnswer(WebsitePage(Index(index))),
        submitAnswer(WebsitePage(Index(index)), Website(s"updated-website-$index")),
      )
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
          answerMustEqual(WebsitePage(Index(0)), Website("website-0")),
          answerMustEqual(WebsitePage(Index(1)), Website("updated-website-1")),
          answerMustEqual(WebsitePage(Index(2)), Website("updated-website-2")),
          answerMustEqual(WebsitePage(Index(3)), Website("website-3"))
        )
    }
  }
}
