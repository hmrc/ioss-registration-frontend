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
import forms.tradingNames.HasTradingNameFormProvider
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.tradingNames.HasTradingNamePage
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.tradingNames.HasTradingNameView


class HasTradingNameControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints

  private val registeredCompanyName = "Company name"

  val formProvider = new HasTradingNameFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val hasTradingNameRoute: String = routes.HasTradingNameController.onPageLoad(waypoints).url

  "HasTradingName Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, hasTradingNameRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HasTradingNameView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, registeredCompanyName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = basicUserAnswersWithVatInfo.set(HasTradingNamePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, hasTradingNameRoute)

        val view = application.injector.instanceOf[HasTradingNameView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints, registeredCompanyName)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted with organisation name populated in the VAT details" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, hasTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(HasTradingNamePage, true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe HasTradingNamePage.navigate(waypoints, basicUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted with individual name populated in the VAT details" in {

      val individualName = "Individual name"
      val vatCustomerInfoWithIndividualName = vatCustomerInfo.copy(organisationName = None, individualName = Some(individualName))
      val userAnswers = basicUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithIndividualName))

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, hasTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = userAnswers.set(HasTradingNamePage, true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe HasTradingNamePage.navigate(waypoints, userAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, hasTradingNameRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[HasTradingNameView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, registeredCompanyName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, hasTradingNameRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, hasTradingNameRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must fail with an exception when both organisationName and individualName are missing in VAT details" in {

      val vatCustomerInfoWithMissingNames = vatCustomerInfo.copy(organisationName = None, individualName = None)
      val userAnswers = basicUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithMissingNames))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, hasTradingNameRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[IllegalStateException]
          exception.getMessage mustBe "Both organisationName and individualName are both missing"
        }
      }
    }

    "must redirect to Journey Recovery when vatInfo is None" in {

      val userAnswers = basicUserAnswersWithVatInfo.copy(vatInfo = None)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, hasTradingNameRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
