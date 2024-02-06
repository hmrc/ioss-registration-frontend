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
import forms.previousRegistrations.PreviousIossNumberFormProvider
import models.core.{Match, MatchType}
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.NonCompliantDetails
import models.{Country, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage, PreviousIossSchemePage, PreviousSchemePage}
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.NonCompliantQuery
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousIossNumberView

import scala.concurrent.Future

class PreviousIossNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val allNonAmendModeWaypoints = Table(
    ("description", "non amend waypoints", "registrion lookup count"),
    ("empty waypoints", EmptyWaypoints, 0),
    ("check answers waypoints", createCheckModeWayPoint(CheckYourAnswersPage), 0),
  )

  private val allModeModeWaypoints = allNonAmendModeWaypoints ++
    List(Tuple3("change registration waypoints", createCheckModeWayPoint(ChangeRegistrationPage), 1))

  val formProvider = new PreviousIossNumberFormProvider()

  private val index = Index(0)
  private val country = Country.euCountries.head
  private val baseAnswers = emptyUserAnswers
    .set(PreviousEuCountryPage(index), country).success.value
    .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
    .set(PreviousIossSchemePage(index, index), false).success.value

  private def previousIossNumberRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(waypoints, index, index).url

  private def previousIossNumberSubmitRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviousIossNumberController.onSubmit(waypoints, index, index).url

  private val hasIntermediary: Boolean = false

  private val form = formProvider(country, hasIntermediary)

  private val ossHintText = "This will start with IM040 followed by 7 numbers"

  "PreviousIossNumber Controller" - {

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

          val request = FakeRequest(GET, previousIossNumberRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[PreviousIossNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, index, index, country,
            hasIntermediary = false, ossHintText, "")(request, messages(application)).toString

          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = baseAnswers
        .set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("answer", None)).success.value

      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
          Mockito.reset(mockRegistrationConnector)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request = FakeRequest(GET, previousIossNumberRoute(waypoints))

          val view = application.injector.instanceOf[PreviousIossNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form.fill(
              PreviousSchemeNumbers("answer", None)), waypoints, index, index, country, hasIntermediary = false, ossHintText, ""
            )(request, messages(application)).toString

          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
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
            FakeRequest(POST, previousIossNumberRoute(waypoints))
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "continue normally when active IOSS found" in {
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
            FakeRequest(POST, previousIossNumberRoute(waypoints))
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
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

      "Redirect to scheme still active when active IOSS found when not in Amend mode" in {
        val countryCode = genericMatch.memberState

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .build()

        running(application) {
          forAll(allNonAmendModeWaypoints) { (_, nonAmendModeWaypoints, _) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(Some(genericMatch))

            val request =
              FakeRequest(POST, previousIossNumberSubmitRoute(nonAmendModeWaypoints))
                .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(
                EmptyWaypoints,
                countryCode
              ).url
            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
          }
        }
      }

      "Allow found IOSS when in Amend mode" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(None)
        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val amendModeWaypoints = createCheckModeWayPoint(ChangeRegistrationPage)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossNumberRoute(amendModeWaypoints))
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(amendModeWaypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(1)).getRegistration()(any())
        }
      }

      "Redirect to scheme quarantined when quarantined IOSS found when not in Amend mode" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .build()

        running(application) {
          forAll(allNonAmendModeWaypoints) { (_, nonAmendModeWaypoints, _) =>
            Mockito.reset(mockSessionRepository)
            Mockito.reset(mockCoreRegistrationValidationService)

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
              Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))

            val request =
              FakeRequest(POST, previousIossNumberSubmitRoute(nonAmendModeWaypoints))
                .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad(
                EmptyWaypoints
              ).url
            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
          }
        }
      }

      "Allow quarantined IOSS found when in Amend mode" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Future.successful(Some(genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)))
        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val amendModeWaypoints = createCheckModeWayPoint(ChangeRegistrationPage)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, previousIossNumberRoute(amendModeWaypoints))
              .withFormUrlEncodedBody(("previousSchemeNumber", "IM0401234567"))

          val result = route(application, request).value
          val expectedAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567", None)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(amendModeWaypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(1)).getRegistration()(any())
        }
      }


      "must save non-compliant details from the active scheme and redirect to the next page when match type is TransferringMSID" in {
        val userAnswers = emptyUserAnswers
          .set(PreviousEuCountryPage(index), country).success.value
          .set(PreviousSchemePage(index, index), PreviousScheme.IOSSWOI).success.value
          .set(PreviousIossSchemePage(index, index), false).success.value

        val previousIossSchemeNumber: String = "IM0401234567"
        val transferringMsidMatch = genericMatch.copy(matchType = MatchType.TransferringMSID, nonCompliantReturns = Some(1), nonCompliantPayments = Some(1))

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
        val mockRegistrationConnector = mock[RegistrationConnector]

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
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
            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
              Some(transferringMsidMatch).toFuture
            when(mockRegistrationConnector.getRegistration()(any()))
              .thenReturn(Future.successful(Right(registrationWrapper)))

            val request =
              FakeRequest(POST, previousIossNumberSubmitRoute(waypoints))
                .withFormUrlEncodedBody(("previousSchemeNumber", previousIossSchemeNumber))

            val result = route(application, request).value

            val expectedAnswers = userAnswers
              .set(PreviousIossNumberPage(index, index),
                PreviousSchemeNumbers(previousSchemeNumber = previousIossSchemeNumber, previousIntermediaryNumber = None)).success.value
              .set(NonCompliantQuery(index, index),
                NonCompliantDetails(nonCompliantReturns = Some(1), nonCompliantPayments = Some(1))
              ).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual PreviousIossNumberPage(index, index).navigate(waypoints, userAnswers, expectedAnswers).url
            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
            verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
          }
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      running(application) {
        forAll(allModeModeWaypoints) { (_, waypoints, registrationCallCount) =>
          Mockito.reset(mockRegistrationConnector)
          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request =
            FakeRequest(POST, previousIossNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[PreviousIossNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual
            view(boundForm, waypoints, index, index, country, hasIntermediary = false, ossHintText, "")(request, messages(application)).toString

          verify(mockRegistrationConnector, times(registrationCallCount)).getRegistration()(any())
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute(EmptyWaypoints))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute(EmptyWaypoints))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
