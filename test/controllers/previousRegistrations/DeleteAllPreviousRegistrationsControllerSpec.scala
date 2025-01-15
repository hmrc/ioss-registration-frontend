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
import forms.previousRegistrations.DeleteAllPreviousRegistrationsFormProvider
import models.domain.PreviousSchemeNumbers
import models.{Country, Index}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.{DeleteAllPreviousRegistrationsPage, PreviousEuCountryPage, PreviousOssNumberPage, PreviouslyRegisteredPage}
import pages.{CheckYourAnswersPage, EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.AllPreviousRegistrationsQuery
import repositories.AuthenticatedUserAnswersRepository
import views.html.previousRegistrations.DeleteAllPreviousRegistrationsView

import scala.concurrent.Future

class DeleteAllPreviousRegistrationsControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val formProvider = new DeleteAllPreviousRegistrationsFormProvider()
  private val form = formProvider()
  private val waypoints: Waypoints = EmptyWaypoints

  private def deleteAllPreviousRegistrationsRoute(waypoints: Waypoints): String =
    routes.DeleteAllPreviousRegistrationsController.onPageLoad(waypoints).url

  private val userAnswers = basicUserAnswersWithVatInfo
    .set(PreviousEuCountryPage(Index(0)), Country("DE", "Germany")).success.value
    .set(PreviousOssNumberPage(Index(0), Index(0)), PreviousSchemeNumbers("DE123", None)).success.value

  private val nonAmendModeWayPoints = Table(
    ("description", "non amend waypoints"),
    ("empty waypoints", EmptyWaypoints),
    ("check answers waypoints", createCheckModeWayPoint(CheckYourAnswersPage))
  )

  "DeleteAllPreviousRegistrations Controller" - {

    "must return OK and the correct view for a GET when NOT in Amend mode" in {
      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        forAll(nonAmendModeWayPoints) { case (_, waypoints: Waypoints) =>
          val request = FakeRequest(GET, deleteAllPreviousRegistrationsRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeleteAllPreviousRegistrationsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints)(request, messages(application)).toString
        }
      }
    }

    "must fail for a GET when in Amend mode" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.getRegistration()(any()))
        .thenReturn(Future.successful(Right(registrationWrapper)))

      running(application) {
        val request = FakeRequest(GET, deleteAllPreviousRegistrationsRoute(createCheckModeWayPoint(ChangeRegistrationPage)))
        route(application, request).value.failed.futureValue mustBe an[InvalidAmendModeOperationException]
      }

      verify(mockRegistrationConnector).getRegistration()(any())
    }

    "must delete all previous registration answers and redirect to the next page when the user answers Yes and not in Amend mode" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        forAll(nonAmendModeWayPoints) { case (_, nonAmendModeWayPoints) =>

          Mockito.reset(mockSessionRepository)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val request =
            FakeRequest(POST, deleteAllPreviousRegistrationsRoute(nonAmendModeWayPoints))
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          val expectedAnswers = userAnswers
            .set(DeleteAllPreviousRegistrationsPage, true).success.value
            .remove(AllPreviousRegistrationsQuery).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            DeleteAllPreviousRegistrationsPage.navigate(nonAmendModeWayPoints, emptyUserAnswers, expectedAnswers).url

          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must not delete all previous registration answers and redirect to the next page when the user answers No when not in Amend mode" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        forAll(nonAmendModeWayPoints) { case (_, nonAmendModeWayPoints) =>
          Mockito.reset(mockRegistrationConnector)
          Mockito.reset(mockSessionRepository)

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request =
            FakeRequest(POST, deleteAllPreviousRegistrationsRoute(nonAmendModeWayPoints))
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value
          val expectedAnswers = userAnswers
            .set(DeleteAllPreviousRegistrationsPage, false).success.value
            .set(PreviouslyRegisteredPage, true).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual DeleteAllPreviousRegistrationsPage.navigate(nonAmendModeWayPoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(0)).getRegistration()(any())
        }
      }
    }

    "must not allow any delete all operations when in Amend mode" in {
      val dpDeleteOptions = Table(
        "do delete",
        "true",
        "false"
      )

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        forAll(dpDeleteOptions) { case (doDelete) =>
          Mockito.reset(mockSessionRepository)
          Mockito.reset(mockRegistrationConnector)

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockRegistrationConnector.getRegistration()(any()))
            .thenReturn(Future.successful(Right(registrationWrapper)))

          val request =
            FakeRequest(POST, deleteAllPreviousRegistrationsRoute(createCheckModeWayPoint(ChangeRegistrationPage)))
              .withFormUrlEncodedBody(("value", doDelete))

          val result = route(application, request).value

          result.failed.futureValue mustBe an[InvalidAmendModeOperationException]
        }
      }
    }


    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllPreviousRegistrationsRoute(EmptyWaypoints))
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAllPreviousRegistrationsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllPreviousRegistrationsRoute(EmptyWaypoints))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllPreviousRegistrationsRoute(EmptyWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
