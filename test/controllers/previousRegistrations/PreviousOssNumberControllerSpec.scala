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
import forms.previousRegistrations.PreviousOssNumberFormProvider
import models.{Country, CountryWithValidationDetails, Index, PreviousScheme}
import models.core.{Match, MatchType}
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.PreviousSchemeHintText
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import views.html.previousRegistrations.PreviousOssNumberView

import scala.concurrent.Future

class PreviousOssNumberControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)
  private val country = Country("SI", "Slovenia")
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == "SI").value
  private val formProvider = new PreviousOssNumberFormProvider()
  private val form = formProvider(country, Seq.empty)
  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val previousOssNumberRoute = controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(waypoints, index, index).url

  private val baseAnswers = basicUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  "PreviousOssNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousOssNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousOssNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, index, countryWithValidation,
          PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("answer", None)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousOssNumberRoute)

        val view = application.injector.instanceOf[PreviousOssNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), waypoints, index, index, countryWithValidation,
          PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" - {

      "when the ID starts with EU it sets to non-union" in {
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
            FakeRequest(POST, previousOssNumberRoute)
              .withFormUrlEncodedBody(("value", "EU123456789"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers
            .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("EU123456789", None)).success.value
            .set(PreviousSchemePage(index, index), PreviousScheme.OSSNU).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "when the ID doesn't start with EU it sets to union" in {
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
            FakeRequest(POST, previousOssNumberRoute)
              .withFormUrlEncodedBody(("value", "SI12345678"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers
            .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("SI12345678", None)).success.value
            .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must deal with core validation responses" - {
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

      "Redirect to scheme still active when active OSS found" in {

        val countryCode = genericMatch.memberState

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(Some(genericMatch))
        when(mockCoreRegistrationValidationService.isActiveTrader(any())) thenReturn true

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute)
              .withFormUrlEncodedBody(("value", "SI12345678"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(waypoints, countryCode, index, index).url
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
        }
      }

      "Redirect to scheme quarantined when quarantined OSS found" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))
        when(mockCoreRegistrationValidationService.isActiveTrader(any())) thenReturn false
        when(mockCoreRegistrationValidationService.isQuarantinedTrader(any())) thenReturn true

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute)
              .withFormUrlEncodedBody(("value", "SI12345678"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
        }
      }

      "not call core validation when OSS Non Union" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))
        when(mockCoreRegistrationValidationService.isActiveTrader(any())) thenReturn false
        when(mockCoreRegistrationValidationService.isQuarantinedTrader(any())) thenReturn true

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute)
              .withFormUrlEncodedBody(("value", "EU123456789"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers
            .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("EU123456789", None)).success.value
            .set(PreviousSchemePage(index, index), PreviousScheme.OSSNU).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, baseAnswers, expectedAnswers).url
          verify(mockCoreRegistrationValidationService, times(0)).searchScheme(any(), any(), any(), any())(any(), any())
        }

      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousOssNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviousOssNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index, index, countryWithValidation,
          PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousOssNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousOssNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
