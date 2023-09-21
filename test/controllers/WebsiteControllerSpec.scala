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

package controllers

import base.SpecBase
import forms.WebsiteFormProvider
import models.{Index, UserAnswers, Website}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import pages.website.WebsitePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.WebsiteView

import scala.concurrent.Future

class WebsiteControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)

  private val formProvider = new WebsiteFormProvider()
  private val form = formProvider(index, Seq.empty)

  private lazy val websiteRoute = website.routes.WebsiteController.onPageLoad(EmptyWaypoints, index).url

  "Website Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, websiteRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WebsiteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, EmptyWaypoints, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WebsitePage(Index(0)), Website("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, websiteRoute)

        val view = application.injector.instanceOf[WebsiteView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), EmptyWaypoints, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, websiteRoute)
            .withFormUrlEncodedBody(("value", "www.example.com"))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(WebsitePage(index), Website("www.example.com")).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual WebsitePage(index).navigate(EmptyWaypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, websiteRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WebsiteView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, EmptyWaypoints, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, websiteRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, websiteRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return NOT_FOUND for a GET with an index of position 10 or greater" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()
      val highIndex = Gen.choose(10, Int.MaxValue).map(Index(_)).sample.value

      running(application) {

        val request = FakeRequest(GET, website.routes.WebsiteController.onPageLoad(EmptyWaypoints, highIndex).url)

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }

    "must return NOT_FOUND for a POST with an index of position 10 or greater" in {

      val answers =
        basicUserAnswersWithVatInfo
          .set(WebsitePage(Index(0)), Website("foo")).success.value
          .set(WebsitePage(Index(1)), Website("foo")).success.value
          .set(WebsitePage(Index(2)), Website("foo")).success.value
          .set(WebsitePage(Index(3)), Website("foo")).success.value
          .set(WebsitePage(Index(4)), Website("foo")).success.value
          .set(WebsitePage(Index(5)), Website("foo")).success.value
          .set(WebsitePage(Index(6)), Website("foo")).success.value
          .set(WebsitePage(Index(7)), Website("foo")).success.value
          .set(WebsitePage(Index(8)), Website("foo")).success.value
          .set(WebsitePage(Index(9)), Website("foo")).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()
      val highIndex = Gen.choose(10, Int.MaxValue).map(Index(_)).sample.value

      running(application) {

        val request =
          FakeRequest(POST, website.routes.WebsiteController.onPageLoad(EmptyWaypoints, highIndex).url)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }
  }
}
