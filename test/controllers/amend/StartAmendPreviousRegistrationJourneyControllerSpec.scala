/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.amend

import base.SpecBase
import connectors.RegistrationConnector
import models.UserAnswers
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.ChangePreviousRegistrationPage
import pages.{EmptyWaypoints, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.PreviousRegistrationIossNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import utils.FutureSyntax.FutureOps

class StartAmendPreviousRegistrationJourneyControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  override val iossNumber: String = arbitrary[String].sample.value

  private val answers: UserAnswers = completeUserAnswersWithVatInfo.set(PreviousRegistrationIossNumberQuery, iossNumber).success.value
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]

  "StartAmendJourney Controller" - {

    "must redirect to Change Previous Registration when a registration wrapper has been successfully retrieved" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockRegistrationConnector.getRegistration(any())(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn answers.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(answers), clock = Some(stubClockAtArbitraryDate))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartAmendPreviousRegistrationJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe ChangePreviousRegistrationPage.route(waypoints).url
      }
    }

    "must redirect to Not Registered Page when no registration found" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(stubClockAtArbitraryDate))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartAmendPreviousRegistrationJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        if (registrationWrapper == null) {
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "must throw an exception when registration connector returns Left(error)" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Left(InternalServerError).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(stubClockAtArbitraryDate))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartAmendPreviousRegistrationJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value.failed

        whenReady(result) { exp =>
          exp mustBe a[Exception]
        }
      }
    }
  }
}
