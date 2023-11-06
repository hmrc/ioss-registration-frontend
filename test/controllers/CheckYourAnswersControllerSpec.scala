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
import models.audit.{RegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.requests.AuthenticatedDataRequest
import models.responses.etmp.EtmpEnrolmentResponse
import models.responses.{ConflictFound, InternalServerError => ServerError}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.filters.CannotRegisterAlreadyRegisteredPage
import pages.{ApplicationCompletePage, CheckYourAnswersPage, EmptyWaypoints, ErrorSubmittingRegistrationPage, Waypoint, Waypoints}
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.etmp.EtmpEnrolmentResponseQuery
import repositories.AuthenticatedUserAnswersRepository
import services.{AuditService, RegistrationService}
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockAuditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    reset(mockRegistrationService)
    reset(mockAuditService)

    super.beforeEach()
  }

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

      "must save the answer and audit the event then redirect to the correct page when a successful registration request returns a valid response body" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          implicit val dataRequest: AuthenticatedDataRequest[AnyContentAsEmpty.type] =
            AuthenticatedDataRequest(request, testCredentials, vrn, completeUserAnswersWithVatInfo)

          val expectedAuditEvent = RegistrationAuditModel.build(
            RegistrationAuditType.CreateRegistration, completeUserAnswersWithVatInfo, Some(etmpEnrolmentResponse), SubmissionResult.Success
          )

          val expectedAnswers = completeUserAnswersWithVatInfo
            .set(EtmpEnrolmentResponseQuery, etmpEnrolmentResponse).success.value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe ApplicationCompletePage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must audit the event and redirect to the correct page when back end returns ConflictFound" in {

        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Left(ConflictFound).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          implicit val dataRequest: AuthenticatedDataRequest[AnyContentAsEmpty.type] =
            AuthenticatedDataRequest(request, testCredentials, vrn, completeUserAnswersWithVatInfo)

          val expectedAuditEvent = RegistrationAuditModel.build(
            RegistrationAuditType.CreateRegistration, completeUserAnswersWithVatInfo, None, SubmissionResult.Duplicate
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe CannotRegisterAlreadyRegisteredPage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      // TODO Add SaveAndContinue when created
      "must audit the event and redirect to the correct page when back end returns any other Error Response" in {

        when(mockRegistrationService.createRegistrationRequest(any(), any())(any())) thenReturn Left(ServerError).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          implicit val dataRequest: AuthenticatedDataRequest[AnyContentAsEmpty.type] =
            AuthenticatedDataRequest(request, testCredentials, vrn, completeUserAnswersWithVatInfo)

          val expectedAuditEvent = RegistrationAuditModel.build(
            RegistrationAuditType.CreateRegistration, completeUserAnswersWithVatInfo, None, SubmissionResult.Failure
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe ErrorSubmittingRegistrationPage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }
  }
}
