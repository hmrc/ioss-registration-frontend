/*
 * Copyright 2026 HM Revenue & Customs
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
import models.intermediaries.EtmpDisplayRegistration
import models.{BankDetails, Bic, CompositeAccount, Iban}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{BankDetailsPage, EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import testutils.GenerateCompositeAccount.generateCompositeAccount
import uk.gov.hmrc.auth.core.Enrolments
import views.html.BankDetailsView

import scala.concurrent.Future

class BankDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new BankDetailsFormProvider()
  private val form = formProvider()
  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val bankDetailsRoute = routes.BankDetailsController.onPageLoad().url

  private val compositeAccount: Option[CompositeAccount] = generateCompositeAccount(ossRegistration)

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

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, None, 0)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(bankDetails), waypoints, None, 0)(request, messages(application)).toString
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

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad().url
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

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, None, 0)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsRoute)
            .withFormUrlEncodedBody(("accountName", "account name"), ("bic", genBic.toString), ("iban", genIban.toString))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET when Oss Registration is present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), compositeAccount = compositeAccount).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = ossRegistration.value.bankDetails.accountName,
          bic = ossRegistration.value.bankDetails.bic,
          iban = ossRegistration.value.bankDetails.iban
        )

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(expectedBankDetails), waypoints, compositeAccount, 0)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Intermediary Registration is present" in {

      val intermediaryRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

      val compositeAccount: Option[CompositeAccount] = generateCompositeAccount(intermediaryRegistration = Some(intermediaryRegistration))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), compositeAccount = compositeAccount).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = intermediaryRegistration.bankDetails.accountName,
          bic = intermediaryRegistration.bankDetails.bic,
          iban = intermediaryRegistration.bankDetails.iban
        )

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(expectedBankDetails), waypoints, compositeAccount, 0)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Oss Registration and Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), compositeAccount = compositeAccount, numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = ossRegistration.value.bankDetails.accountName,
          bic = ossRegistration.value.bankDetails.bic,
          iban = ossRegistration.value.bankDetails.iban
        )

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(expectedBankDetails), waypoints, compositeAccount, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when 1 previous Ioss registration is present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, None, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when more than 1 Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), numberOfIossRegistrations = 2).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, None, 2)(request, messages(application)).toString
      }
    }
  }
}
