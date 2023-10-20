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
import models.CheckMode
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint}
import models.responses.etmp.EtmpEnrolmentResponse
import models.responses.{ConflictFound, InternalServerError => ServerError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.filters.CannotRegisterAlreadyRegisteredPage
import pages.{ApplicationCompletePage, EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RegistrationService
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time.LocalDateTime

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  "Check Your Answers Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustBe OK

        contentAsString(result) mustBe view(waypoints, list, list, isValid = false)(request, messages(application)).toString
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the correct page when a successful registration request returns a valid response body" in {

        val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(),
          formBundleNumber = None,
          vrn = vrn.vrn,
          iossReference = "",
          businessPartner = ""
        )

        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe ApplicationCompletePage.route(waypoints).url
        }
      }

      "must redirect to the correct page when back end returns ConflictFound" in {

        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Left(ConflictFound).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual CannotRegisterAlreadyRegisteredPage.route(waypoints).url
        }
      }

      // TODO Add SaveAndContinue when created
      // This will redirect to ErrorSubmittingRegistrationPage
      "must return InternalServerError when back end returns InternalServerError" in {

        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Left(ServerError).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints).url)

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe Json.toJson(s"An internal server error occurred with error: ${ServerError.body}")
        }
      }
    }
  }
}
