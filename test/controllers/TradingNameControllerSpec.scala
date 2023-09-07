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
import forms.TradingNameFormProvider
import models.{Index, TradingName}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, JourneyRecoveryPage, TradingNamePage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.TradingNameView

import scala.concurrent.Future

class TradingNameControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val companyName: String = "Company name"
  private val index: Index = Index(0)
  private val highIndex = Gen.choose(10, Int.MaxValue).map(Index(_)).sample.value

  val formProvider = new TradingNameFormProvider()
  val form: Form[String] = formProvider(index, Seq.empty)

  lazy val tradingNameRoute: String = routes.TradingNameController.onPageLoad(waypoints, index).url

  lazy val tradingNameHighIndexRoute: String = routes.TradingNameController.onPageLoad(waypoints, highIndex).url

  "TradingName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, tradingNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TradingNameView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = basicUserAnswersWithVatInfo.set(TradingNamePage(index), TradingName("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, tradingNameRoute)

        val view = application.injector.instanceOf[TradingNameView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill("answer"), waypoints, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameRoute)
            .withFormUrlEncodedBody(("value", companyName))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(TradingNamePage(index), TradingName(companyName)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe TradingNamePage(index).navigate(waypoints, emptyUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[TradingNameView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, tradingNameRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must return NOT_FOUND for a GET with an index of position 10 or greater" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {

        val request = FakeRequest(GET, tradingNameHighIndexRoute)

        val result = route(application, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "must return NOT_FOUND for a POST with an index of position 10 or greater" in {

      val answers =
        basicUserAnswersWithVatInfo
          .set(TradingNamePage(Index(0)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(1)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(2)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(3)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(4)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(5)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(6)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(7)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(8)), TradingName("foo")).success.value
          .set(TradingNamePage(Index(9)), TradingName("foo")).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {

        val request =
          FakeRequest(POST, tradingNameHighIndexRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustBe NOT_FOUND
      }
    }
  }
}
