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
import connectors.RegistrationConnector
import controllers.routes
import forms.previousRegistrations.PreviousOssNumberFormProvider
import models.core.{Match, MatchType}
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.PreviousSchemeHintText
import models.{Country, CountryWithValidationDetails, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage}
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import views.html.previousRegistrations.PreviousOssNumberView

import scala.concurrent.Future

class PreviousOssNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val allNonAmendModeWaypoints = Table(
    ("description", "non amend waypoints", "registrion lookup count"),
    ("empty waypoints", EmptyWaypoints, 0),
    ("check answers waypoints", createCheckModeWayPoint(CheckYourAnswersPage), 0),
  )

  private val allModeModeWaypoints = allNonAmendModeWaypoints ++
    List(Tuple3("change registration waypoints", createCheckModeWayPoint(ChangeRegistrationPage), 1))

  private val index = Index(0)
  private val country = Country("SI", "Slovenia")
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == "SI").value
  private val formProvider = new PreviousOssNumberFormProvider()
  private val form = formProvider(country, Seq.empty)

  private def previousOssNumberRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(waypoints, index, index).url

  private val baseAnswers = basicUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  "PreviousOssNumber Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
          Mockito.reset(mockRegistrationConnector)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[PreviousOssNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form, waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = baseAnswers.set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("answer", None)).success.value

      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
          Mockito.reset(mockRegistrationConnector)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form.fill("answer"), waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" - {

      "when the ID starts with EU it sets to non-union" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

        running(application) {
          forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)
            Mockito.reset(mockRegistrationConnector)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(None)
            when(mockRegistrationConnector.getRegistration()(any()))
              .thenReturn(Future.successful(Right(registrationWrapper)))

            val request =
              FakeRequest(POST, previousOssNumberRoute(waypoints))
                .withFormUrlEncodedBody(("value", "EU123456789"))

            val result = route(application, request).value
            val expectedAnswers = baseAnswers
              .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("EU123456789", None)).success.value
              .set(PreviousSchemePage(index, index), PreviousScheme.OSSNU).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
            verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
          }
        }
      }

      "when the ID doesn't start with EU it sets to union" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

        running(application) {
          forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)
            Mockito.reset(mockRegistrationConnector)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(None)
            when(mockRegistrationConnector.getRegistration()(any()))
              .thenReturn(Future.successful(Right(registrationWrapper)))

            val request =
              FakeRequest(POST, previousOssNumberRoute(waypoints))
                .withFormUrlEncodedBody(("value", "SI12345678"))

            val result = route(application, request).value
            val expectedAnswers = baseAnswers
              .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("SI12345678", None)).success.value
              .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
            verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
          }
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

      "Continue on normal journey if to scheme still active when active OSS found" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {
          forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)
            Mockito.reset(mockRegistrationConnector)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
              Future.successful(Some(genericMatch))
            when(mockRegistrationConnector.getRegistration()(any()))
              .thenReturn(Future.successful(Right(registrationWrapper)))

            val request =
              FakeRequest(POST, previousOssNumberRoute(waypoints))
                .withFormUrlEncodedBody(("value", "SI12345678"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(
                waypoints,
                index
              ).url
            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
            verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
          }
        }
      }

      "Redirect to scheme quarantined when quarantined OSS found when not in Amend mode" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {
          forAll(allNonAmendModeWaypoints) { (_, waypoints, _) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
              Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))

            val request =
              FakeRequest(POST, previousOssNumberRoute(waypoints))
                .withFormUrlEncodedBody(("value", "SI12345678"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(waypoints).toString

            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
          }
        }
      }

      "Allow quarantined OSS when in Amend Mode" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val amendModeWaypoints = createCheckModeWayPoint(ChangeRegistrationPage)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .configure(
              "features.other-country-reg-validation-enabled" -> true
            )
            .build()

        running(application) {

          val request =
            FakeRequest(POST, previousOssNumberRoute(amendModeWaypoints))
              .withFormUrlEncodedBody(("value", "SI12345678"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(amendModeWaypoints, emptyUserAnswers, baseAnswers).url

          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
          verify(mockRegistrationConnector, times(1)).getRegistration()(any())
        }
      }


      "not call core validation when OSS Non Union" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

        running(application) {
          forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)
            Mockito.reset(mockRegistrationConnector)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockRegistrationConnector.getRegistration()(any()))
              .thenReturn(Future.successful(Right(registrationWrapper)))

            val request =
              FakeRequest(POST, previousOssNumberRoute(waypoints))
                .withFormUrlEncodedBody(("value", "EU123456789"))

            val result = route(application, request).value
            val expectedAnswers = baseAnswers
              .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("EU123456789", None)).success.value
              .set(PreviousSchemePage(index, index), PreviousScheme.OSSNU).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual PreviousOssNumberPage(index, index).navigate(waypoints, baseAnswers, expectedAnswers).url
            verify(mockCoreRegistrationValidationService, times(0)).searchScheme(any(), any(), any(), any())(any(), any())
            verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
          }
        }

      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
          Mockito.reset(mockRegistrationConnector)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, index, index, countryWithValidation,
            PreviousSchemeHintText.Both)(request, messages(application)).toString

          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, _) =>
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, _) =>
          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
