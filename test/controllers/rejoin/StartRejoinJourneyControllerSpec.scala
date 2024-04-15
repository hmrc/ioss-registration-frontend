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

package controllers.rejoin

import base.SpecBase
import connectors.{RegistrationConnector, ReturnStatusConnector}
import controllers.rejoin.validation.RejoinRegistrationValidation
import models.{CheckMode, CurrentReturns}
import models.amend.RegistrationWrapper
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.mockito.{ArgumentMatchers, IdiomaticMockito, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.rejoin.{CannotRejoinRegistrationPage, RejoinRegistrationPage}
import pages.{EmptyWaypoints, NonEmptyWaypoints, Waypoint, Waypoints}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import scala.concurrent.Future

class StartRejoinJourneyControllerSpec extends SpecBase with BeforeAndAfterEach with TableDrivenPropertyChecks with IdiomaticMockito {

  private val waypoints: Waypoints = EmptyWaypoints

  private val rejoinWaypoints: NonEmptyWaypoints =
    EmptyWaypoints.setNextWaypoint(Waypoint(RejoinRegistrationPage, CheckMode, RejoinRegistrationPage.urlFragment))

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val mockRejoinRegistrationValidation = mock[RejoinRegistrationValidation]
  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockRegistrationService)
    Mockito.reset(mockAuthenticatedUserAnswersRepository)
    Mockito.reset(mockRejoinRegistrationValidation)
    Mockito.reset(mockReturnStatusConnector)
  }

  "StartRejoinJourney Controller" - {

    "must redirect to Rejoin Registration when a registration wrapper has been successfully retrieved and is passes exclusion sanity checks" in {
      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      when(mockRejoinRegistrationValidation.validateEuRegistrations(
        ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
        ArgumentMatchers.eq(rejoinWaypoints)
      )(any(),any(),any())) thenReturn Right(true).toFuture

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

      val application = applicationBuilder(
        userAnswers = Some(completeUserAnswersWithVatInfo),
        clock = Some(Clock.systemUTC())
      )
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .overrides(bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartRejoinJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RejoinRegistrationPage.route(waypoints).url
      }
    }

    def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate): RegistrationWrapper = {
      val exclusion = EtmpExclusion(
        exclusionReason = NoLongerSupplies,
        effectiveDate = effectiveDate,
        decisionDate = LocalDate.now(),
        quarantine = false
      )

      val registration = registrationWrapper.registration
      registrationWrapper.copy(registration = registration.copy(exclusions = List(exclusion)))
    }


    "must redirect to Rejoin Registration when a registration wrapper has been successfully retrieved but it does not pass exclusion sanity checks" in {
      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now().plusDays(1))

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture
      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

      val application = applicationBuilder(
        userAnswers = Some(completeUserAnswersWithVatInfo),
        clock = Some(Clock.systemUTC())
      )
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartRejoinJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CannotRejoinRegistrationPage.route(waypoints).url
      }
    }

    "validation on schemeDetails" - {
      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

      def createFilterPassingApplicationApplication(mockRejoinRegistrationValidation: RejoinRegistrationValidation) = {
        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture
        when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
        when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
        when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

        applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC())
        )
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
          .overrides(bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

      }

      "must redirect to Rejoin Registration when a registration wrapper has been successfully retrieved but it has infractions on euRegistrationDetails" in {
          val invalidValidationRedirect = Call("POST", "/redirect-location")

          val mockRejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(mockRejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints))(any(), any(), any())
          ).thenReturn(Future.successful(Left(invalidValidationRedirect)))

          val application = createFilterPassingApplicationApplication(mockRejoinRegistrationValidation)
          running(application) {
            val request = FakeRequest(GET, routes.StartRejoinJourneyController.onPageLoad(rejoinWaypoints).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe invalidValidationRedirect.url
          }

      }
    }


    "must redirect to Not Registered Page when no registration found" in {
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn completeUserAnswersWithVatInfo.toFuture
      when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(
        userAnswers = Some(completeUserAnswersWithVatInfo),
        clock = Some(stubClockAtArbitraryDate)
      )
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartRejoinJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        if (registrationWrapper == null) {
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.NotRegisteredController.onPageLoad().url
        }
      }
    }
  }
}
