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

package controllers.tradingNames

import base.SpecBase
import forms.tradingNames.AddTradingNameFormProvider
import models.{Index, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.tradingNames.{AddTradingNamePage, TradingNamePage}
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.tradingName.TradingNameSummary
import views.html.tradingNames.AddTradingNameView

class AddTradingNameControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val index: Index = Index(0)

  val formProvider = new AddTradingNameFormProvider()
  val form: Form[Boolean] = formProvider()

  private val answers = basicUserAnswersWithVatInfo.set(TradingNamePage(Index(0)), TradingName("foo")).success.value

  lazy val addTradingNameRoute: String = routes.AddTradingNameController.onPageLoad(waypoints).url

  "AddTradingName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, addTradingNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddTradingNameView]

        val list = TradingNameSummary.addToListRows(answers, waypoints, AddTradingNamePage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, list, canAddTradingNames = true, None, 1)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of trading names have already been added" in {

      val userAnswers = (0 to 9).foldLeft(answers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(TradingNamePage(Index(index)), TradingName("foo")).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addTradingNameRoute)

        val view = application.injector.instanceOf[AddTradingNameView]

        val list = TradingNameSummary.addToListRows(userAnswers, waypoints, AddTradingNamePage())

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints, list, canAddTradingNames = false, None, 1)(request, messages(application)).toString
      }
    }

    "must allow adding a trading name when just below the maximum number of trading names" in {
      val userAnswers = (0 to 8).foldLeft(answers) { case (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(TradingNamePage(Index(index)), TradingName("foo")).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addTradingNameRoute)

        val view = application.injector.instanceOf[AddTradingNameView]

        val list = TradingNameSummary.addToListRows(userAnswers, waypoints, AddTradingNamePage())

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, list, canAddTradingNames = true, None, 1)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = answers.set(AddTradingNamePage(Some(index)), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe AddTradingNamePage(Some(index)).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addTradingNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddTradingNameView]

        val list = TradingNameSummary.addToListRows(answers, waypoints, AddTradingNamePage())

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, list, canAddTradingNames = true, None, 1)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addTradingNameRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
