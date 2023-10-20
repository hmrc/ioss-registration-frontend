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

package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.PreviousIossNumberFormProvider
import models.domain.PreviousSchemeNumbers
import models.{Country, Index, PreviousScheme}
import models.core.{Match, MatchType}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage, PreviousIossSchemePage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import views.html.previousRegistrations.PreviousIossNumberView

import scala.concurrent.Future

class PreviousIossNumberControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new PreviousIossNumberFormProvider()

  private val index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints
  private val country = Country.euCountries.head
  private val baseAnswers = emptyUserAnswers
    .set(PreviousEuCountryPage(index), country).success.value
    .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
    .set(PreviousIossSchemePage(index, index), false).success.value

  private lazy val previousIossNumberRoute = controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(waypoints, index, index).url
  private lazy val previousIossNumberSubmitRoute = controllers.previousRegistrations.routes.PreviousIossNumberController.onSubmit(waypoints, index, index).url

  private val hasIntermediary: Boolean = false

  private val form = formProvider(country, hasIntermediary)

  private val ossHintText = "This will start with IM040 followed by 7 numbers"

  "PreviousIossNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousIossNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, index, country,
          hasIntermediary = false, ossHintText, "")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers
        .set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("answer", None)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(PreviousSchemeNumbers("answer", None)),
          waypoints, index, index, country, hasIntermediary = false, ossHintText, "")(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "continue normally when active IOSS found" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }

    }

    "Deal with core validation responses" - {
      val genericMatch = Match(
        MatchType.TraderIdActiveNETP,
        "IM0987654321",
        None,
        "DE",
        None,
        None,
        None,
        None,
        None
      )

      "Redirect to scheme still active when active IOSS found" in {

        val countryCode = genericMatch.memberState

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(Some(genericMatch))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossNumberSubmitRoute)
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(
              waypoints,
              countryCode
            ).url
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
        }
      }

      "Redirect to scheme quarantined when quarantined IOSS found" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossNumberSubmitRoute)
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(
              waypoints
            ).url
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index, index, country,
          hasIntermediary = false, ossHintText, "")(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
