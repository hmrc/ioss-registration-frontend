/*
 * Copyright 2026 HM Revenue & Customs
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

package services.core

import base.SpecBase
import controllers.routes
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import models.UserAnswers
import models.core.{Match, TraderId}
import models.ossExclusions.ExclusionReason
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{EmptyWaypoints, Waypoints}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class CoreSavedAnswersRevalidationServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]
  private val waypoints: Waypoints = EmptyWaypoints
  private val genericMatch = Match(
    TraderId("IM9001234566"),
    None,
    "DE",
    None,
    None,
    None,
    None,
    None
  )


  override def beforeEach(): Unit = {
    Mockito.reset(
      mockCoreRegistrationValidationService,
      mockSessionRepository
    )
  }

  "CoreSavedAnswersRevalidationService" - {

    ".checkAndValidateSavedUserAnswers" - {

      "must return the expired VAT redirect when the deregistration date is today" in {

        val today = LocalDate.now(stubClockAtArbitraryDate)

        val vatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = Some(today))

        val userAnswers = emptyUserAnswers.copy(vatInfo = Some(vatInfo))

        implicit val request: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, userAnswers, None, 1, None)

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

        result mustBe Some(
          Redirect(routes.ExpiredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints))
        )

        verifyNoInteractions(mockCoreRegistrationValidationService)
      }

      "must return the already registered redirect when the UK VRN is active" in {

        val userAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))

        implicit val request: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, userAnswers, None, 1, None)

        val activeMatch = genericMatch.copy(
          memberState = "DE",
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

        result mustBe Some(Redirect(controllers.routes.AlreadyRegisteredVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints, activeMatch.memberState)))

        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), eqTo(request))

      }

      "must return the quarantined redirect when the UK VRN is quarantined" in {

        val userAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))

        implicit val request: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, userAnswers, None, 1, None)

        val activeMatch = genericMatch.copy(
          memberState = "DE",
          exclusionStatusCode =  Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

        result mustBe Some(Redirect(controllers.routes.QuarantinedVatCannotBeUsedForSaveAndComeBackController.onPageLoad(waypoints, activeMatch.memberState, activeMatch.getEffectiveDate)))

        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), eqTo(request))

      }

      "must continue checking saved answers when the UK VRN has no match" in {

        val userAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))

        implicit val request: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, userAnswers, None, 1, None)

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

        result mustBe None

        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), eqTo(request))
      }
    }
  }
}
