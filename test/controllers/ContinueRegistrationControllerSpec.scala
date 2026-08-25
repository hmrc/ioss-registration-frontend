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
import connectors.{RegistrationConnector, SaveForLaterConnector}
import forms.ContinueRegistrationFormProvider
import models.ContinueRegistration.{Continue, Delete}
import models.core.{Match, TraderId}
import models.responses
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.ContinueRegistrationView

import java.time.LocalDate
import scala.concurrent.Future

class ContinueRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new ContinueRegistrationFormProvider()
  private val form = formProvider()



  private lazy val continueRegistrationRoute = routes.ContinueRegistrationController.onPageLoad().url
  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  "ContinueRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(responses.NotFound).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(SavedProgressPage, "testUrl").success.value))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ContinueRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to the saved url when Continue submitted" in {

      val userAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val saveForLaterConnector = mock[SaveForLaterConnector]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(SavedProgressPage, "testUrl").success.value))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(userAnswersRepository),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", Continue.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "testUrl"
        verifyNoInteractions(saveForLaterConnector)
        verifyNoInteractions(userAnswersRepository)
      }
    }

    "must redirect to the index page and delete saved answers when Delete submitted" in {

      val userAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val saveForLaterConnector = mock[SaveForLaterConnector]

      when(userAnswersRepository.clear(any())) thenReturn(Future.successful(true))
      when(saveForLaterConnector.delete()(any())) thenReturn(Future.successful(Right(true)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(SavedProgressPage, "testUrl").success.value))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(userAnswersRepository),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", Delete.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad().url
        verify(saveForLaterConnector, times(1)).delete()(any())
        verify(userAnswersRepository, times(1)).clear(any())
      }
    }

    "must redirect to the expired VAT page and delete the saved registration when the VAT registration is expired" in {

      val mockUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]

      val vatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate)))

      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn
        Right(vatInfo).toFuture

      when(mockUserAnswersRepository.clear(any())) thenReturn Future.successful(true)
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Right(true).toFuture

      val application =
        applicationBuilder(
          userAnswers = Some(
            emptyUserAnswers
              .set(SavedProgressPage, "testUrl")
              .success
              .value
          )
        )
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[AuthenticatedUserAnswersRepository].toInstance(mockUserAnswersRepository),
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.ExpiredVatCannotBeUsedForSaveAndComeBackController
            .onPageLoad(EmptyWaypoints)
            .url

        verify(mockUserAnswersRepository, times(1)).clear(any())
        verify(mockSaveForLaterConnector, times(1)).delete()(any())
      }
    }

    "must redirect to the already registered page and delete the saved registration when the VRN belongs to an active trader" in {

      val mockUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]

      val vatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = None)

      val genericMatch = Match(
        TraderId("IM9001234566"),
        None,
        "DE",
        None,
        None,
        None,
        None,
        None
      )

      val activeMatch = genericMatch

      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn
        Right(vatInfo).toFuture

      when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn
        Some(activeMatch).toFuture

      when(mockUserAnswersRepository.clear(any())) thenReturn Future.successful(true)
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Right(true).toFuture

      val application =
        applicationBuilder(
          userAnswers = Some(
            emptyUserAnswers
              .set(SavedProgressPage, "testUrl")
              .success
              .value
          )
        )
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[AuthenticatedUserAnswersRepository].toInstance(mockUserAnswersRepository),
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.AlreadyRegisteredVatCannotBeUsedForSaveAndComeBackController
            .onPageLoad(EmptyWaypoints, activeMatch.memberState)
            .url

        verify(mockUserAnswersRepository, times(1)).clear(any())
        verify(mockSaveForLaterConnector, times(1)).delete()(any())
      }
    }

    "must redirect to the quarantined page and delete the saved registration when the trader is quarantined" in {

      val genericMatch = Match(
        TraderId("IM9001234566"),
        None,
        "DE",
        Some(2),
        None,
        None,
        None,
        None
      )

      val mockUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]

      val vatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = None)

      val expectedMatch = genericMatch.copy(
        exclusionStatusCode = Some(4),
        exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusMonths(6).toString)
      )

      when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatInfo).toFuture

      when(mockUserAnswersRepository.clear(any())) thenReturn Future.successful(true)
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Right(true).toFuture

      val application =
        applicationBuilder(
          userAnswers = Some(
            emptyUserAnswers
              .set(SavedProgressPage, "testUrl")
              .success
              .value
          )
        )
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[AuthenticatedUserAnswersRepository].toInstance(mockUserAnswersRepository),
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.QuarantinedVatCannotBeUsedForSaveAndComeBackController
            .onPageLoad(
              EmptyWaypoints,
              expectedMatch.memberState,
              expectedMatch.getEffectiveDate
            )
            .url

        verify(mockUserAnswersRepository, times(1)).clear(any())
        verify(mockSaveForLaterConnector, times(1)).delete()(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(SavedProgressPage, "testUrl").success.value)).build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ContinueRegistrationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the index page if SavedProgressPage is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery if the form value is valid but SavedProgressPage is missing" in {

      val userAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val saveForLaterConnector = mock[SaveForLaterConnector]
      val waypoints: Waypoints = EmptyWaypoints

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(userAnswersRepository),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", Continue.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
        verifyNoInteractions(saveForLaterConnector)
        verifyNoInteractions(userAnswersRepository)
      }
    }
  }
}

