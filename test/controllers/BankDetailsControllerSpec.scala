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
import forms.BankDetailsFormProvider
import models.{BankDetails, Bic, Iban}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{BankDetailsPage, EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import views.html.BankDetailsView

import scala.concurrent.Future

class BankDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new BankDetailsFormProvider()
  private val form = formProvider()
  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val bankDetailsRoute = routes.BankDetailsController.onPageLoad().url

  private val genBic = arbitrary[Bic].sample.value
  private val genIban = arbitrary[Iban].sample.value
  private val bankDetails = BankDetails("account name", Some(genBic), genIban)
  private val userAnswers = basicUserAnswersWithVatInfo.set(BankDetailsPage, bankDetails).success.value

  "BankDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, None, 0)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(bankDetails), waypoints, None, 1)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, bankDetailsRoute)
            .withFormUrlEncodedBody(("accountName", "account name"), ("bic", genBic.toString), ("iban", genIban.toString))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(BankDetailsPage, bankDetails).success.value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, None, 0)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsRoute)
            .withFormUrlEncodedBody(("accountName", "account name"), ("bic", genBic.toString), ("iban", genIban.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET when Oss Registration is present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = "OSS Account Name",
          bic = Bic("OSSBIC123"),
          iban = Iban("GB33BUKB20201555555555").value
        )

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(expectedBankDetails), waypoints, ossRegistration, 0)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Oss Registration and Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration, numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = "OSS Account Name",
          bic = Bic("OSSBIC123"),
          iban = Iban("GB33BUKB20201555555555").value
        )

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(expectedBankDetails), waypoints, ossRegistration, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when 1 previous Ioss registration is present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = None, numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, None, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when more than 1 Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = None, numberOfIossRegistrations = 2).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, None, 2)(request, messages(application)).toString
      }
    }
  }
}
