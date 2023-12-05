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
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{Country, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{CannotRemoveExistingPreviousRegistrationsPage, CheckYourAnswersPage, EmptyWaypoints, NonEmptyWaypoints, Waypoints}
import pages.previousRegistrations._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.PreviousRegistrationQuery
import repositories.AuthenticatedUserAnswersRepository
import testutils.RegistrationData
import views.html.previousRegistrations.DeletePreviousRegistrationView

import scala.concurrent.Future

class DeletePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val amendModeWaypoints: NonEmptyWaypoints = createCheckModeWayPoint(ChangeRegistrationPage)

  private val nonAmendModeWaypoints = Table(
    ("description", "non amend waypoints"),
    ("empty waypoints", EmptyWaypoints),
    ("check answers waypoints", createCheckModeWayPoint(CheckYourAnswersPage))
  )

  private val formProvider = new DeletePreviousRegistrationFormProvider()
  private val form = formProvider()

  private val index = Index(0)
  private val country = Country.euCountries.head
  private val previousSchemeNumbers = PreviousSchemeNumbers("VAT Number", None)
  private val previousScheme = PreviousSchemeDetails(PreviousScheme.OSSU, previousSchemeNumbers, None)
  private val previousRegistration = PreviousRegistrationDetails(country, List(previousScheme))

  private def deletePreviousRegistrationRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.DeletePreviousRegistrationController.onPageLoad(waypoints, index).url

  private val baseUserAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviousEuCountryPage(index), previousRegistration.previousEuCountry).success.value
      .set(PreviousOssNumberPage(index, index), previousSchemeNumbers).success.value

  private val austrianContainingRegistrationWrapper = registrationWrapper.copy(registration =
    RegistrationData.etmpDisplayRegistration.copy(schemeDetails =
      RegistrationData.etmpSchemeDetails.copy(
        previousEURegistrationDetails =
          RegistrationData.etmpSchemeDetails.previousEURegistrationDetails.map(details => details.copy(issuedBy = "AT"))
      )
    )
  )

  "DeletePreviousRegistration Controller" - {

    "must return OK and the correct view for a GET when not in Amend mode" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>

          val request = FakeRequest(GET, deletePreviousRegistrationRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeletePreviousRegistrationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, index, previousRegistration.previousEuCountry.name)(request, messages(application)).toString
        }
      }
    }


    "must return OK and the correct view for a GET when in Amend mode and the country has not been previously saved" in {
      val mockRegistrationConnector = mock[RegistrationConnector]

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      when(mockRegistrationConnector.getRegistration()(any()))
        .thenReturn(Future.successful(Right(registrationWrapper)))

      running(application) {
        val request = FakeRequest(GET, deletePreviousRegistrationRoute(amendModeWaypoints))

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeletePreviousRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, amendModeWaypoints, index, previousRegistration.previousEuCountry.name)(request, messages(application)).toString
        verify(mockRegistrationConnector, times(1)).getRegistration()(any())
      }
    }

    "must error for a GET when in Amend mode and the country has been previously saved" in {
      val mockRegistrationConnector = mock[RegistrationConnector]

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      when(mockRegistrationConnector.getRegistration()(any()))
        .thenReturn(Future.successful(Right(austrianContainingRegistrationWrapper)))

      running(application) {
        val request = FakeRequest(GET, deletePreviousRegistrationRoute(amendModeWaypoints))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CannotRemoveExistingPreviousRegistrationsPage.route(amendModeWaypoints).url

        verify(mockRegistrationConnector, times(1)).getRegistration()(any())
      }
    }

    "must delete a record and redirect to the next page when the user answers Yes when not in Amend mode" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          Mockito.reset(mockSessionRepository)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val request =
            FakeRequest(POST, deletePreviousRegistrationRoute(waypoints))
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value
          val expectedAnswers =
            baseUserAnswers
              .remove(PreviousRegistrationQuery(index)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual DeletePreviousRegistrationPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must delete a record and redirect to the next page when the user answers Yes when in Amend mode and the country has not been saved" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockRegistrationConnector.getRegistration()(any))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request =
          FakeRequest(POST, deletePreviousRegistrationRoute(amendModeWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers =
          baseUserAnswers
            .remove(PreviousRegistrationQuery(index)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual DeletePreviousRegistrationPage(index)
          .navigate(amendModeWaypoints, emptyUserAnswers, expectedAnswers).url

        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        verify(mockRegistrationConnector, times(1)).getRegistration()(any())
      }
    }

    "must redirect to an error page when trying to delete when the country is in the saved registration list" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockRegistrationConnector.getRegistration()(any))
          .thenReturn(Future.successful(Right(austrianContainingRegistrationWrapper)))

        val request =
          FakeRequest(POST, deletePreviousRegistrationRoute(amendModeWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers =
          baseUserAnswers
            .remove(PreviousRegistrationQuery(index)).success.value

        result.failed.futureValue mustBe a[InvalidAmendModeOperationException]

        verify(mockSessionRepository, times(0)).set(eqTo(expectedAnswers))
        verify(mockRegistrationConnector, times(1)).getRegistration()(any())
      }
    }

    "must not delete a record and redirect to the next page when the user answers No when not in Amend mode" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          val request =
            FakeRequest(POST, deletePreviousRegistrationRoute(waypoints))
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual DeletePreviousRegistrationPage(index).navigate(waypoints, emptyUserAnswers, baseUserAnswers).url
          verify(mockSessionRepository, never()).set(any())
        }
      }
    }


    "must return a Bad Request and errors when invalid data is submitted when not in Amend mode" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          val request =
            FakeRequest(POST, deletePreviousRegistrationRoute(waypoints))
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[DeletePreviousRegistrationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm, waypoints, index, previousRegistration.previousEuCountry.name)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found when not in Amend mode" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          val request = FakeRequest(GET, deletePreviousRegistrationRoute(waypoints))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no EU VAT details exist when not in Amend mode" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          val request = FakeRequest(GET, deletePreviousRegistrationRoute(waypoints))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found  when not in Amend mode" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { (_, waypoints) =>
          val request =
            FakeRequest(POST, deletePreviousRegistrationRoute(waypoints))
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
