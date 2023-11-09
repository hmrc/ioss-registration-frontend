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
import forms.previousRegistrations.PreviouslyRegisteredFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{CheckYourAnswersPage, EmptyWaypoints, NonEmptyWaypoints, Waypoints}
import pages.previousRegistrations.PreviouslyRegisteredPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.previousRegistrations.PreviouslyRegisteredView

import scala.concurrent.Future

class PreviouslyRegisteredControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val formProvider = new PreviouslyRegisteredFormProvider()
  private val form = formProvider()

  private val emptyWaypoints: Waypoints = EmptyWaypoints

  private def previouslyRegisteredRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviouslyRegisteredController.onPageLoad(waypoints).url

  private val nonAmendModeWaypoints = Table(
    ("description", "non amend waypoints"),
    ("empty waypoints", EmptyWaypoints),
    ("check answers waypoints", createCheckModeWayPoint(CheckYourAnswersPage))
  )

  private val amendModeWaypoints: NonEmptyWaypoints = createCheckModeWayPoint(ChangeRegistrationPage)
  private val allModeWaypoints = nonAmendModeWaypoints ++
    List("amend mode waypoints" -> amendModeWaypoints)

  "PreviouslyRegistered Controller" - {

    "must return OK and the correct view for a GET when no question has been answered for all modes" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {

        forAll(allModeWaypoints) { case (_, waypoints) =>

          val request = FakeRequest(GET, previouslyRegisteredRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[PreviouslyRegisteredView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints)(request, messages(application)).toString

        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered as true when not in Amend mode" in {
      val userAnswers = UserAnswers(userAnswersId).set(PreviouslyRegisteredPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        forAll(nonAmendModeWaypoints) { case (_, waypoints) =>
          val request = FakeRequest(GET, previouslyRegisteredRoute(waypoints))

          val view = application.injector.instanceOf[PreviouslyRegisteredView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), waypoints)(request, messages(application)).toString
        }
      }
    }

    "must fail on GET when the existing answer is true when in Amend mode" in {
      val userAnswers = UserAnswers(userAnswersId).set(PreviouslyRegisteredPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previouslyRegisteredRoute(amendModeWaypoints))
        val result = route(application, request).value

        result.failed.futureValue mustBe an[InvalidAmendModeOperationException]
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted when the answer is originally true" in {
      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        forAll(allModeWaypoints) { case (_, waypoints) =>
          Mockito.reset(mockSessionRepository)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val request =
            FakeRequest(POST, previouslyRegisteredRoute(waypoints))
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value
          val expectedAnswers = basicUserAnswersWithVatInfo.set(PreviouslyRegisteredPage, true).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual PreviouslyRegisteredPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, previouslyRegisteredRoute(emptyWaypoints))
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviouslyRegisteredView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previouslyRegisteredRoute(emptyWaypoints))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previouslyRegisteredRoute(emptyWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
