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

package controllers.rejoin.validation

import base.SpecBase
import controllers.euDetails.routes.{ExcludedVRNController, FixedEstablishmentVRNAlreadyRegisteredController}
import controllers.previousRegistrations.routes.{SchemeQuarantinedController, SchemeStillActiveController}
import models.CheckMode
import models.amend.RegistrationWrapper
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.rejoin.RejoinRegistrationPage
import pages.{EmptyWaypoints, Waypoint}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.core.{EuRegistrationsValidationService, InvalidActiveTrader, InvalidQuarantinedTrader}
import testutils.RegistrationData.{etmpDisplayEuRegistrationDetails, etmpEuPreviousRegistrationDetails}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RejoinRegistrationValidationSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val mockEuRegistrationsValidationService: EuRegistrationsValidationService = mock[EuRegistrationsValidationService]
  private val rejoinRegistrationValidation = new RejoinRegistrationValidation(mockEuRegistrationsValidationService)
  private val rejoinWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinRegistrationPage, CheckMode, ""))

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)
  implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

  private val registrationWrapperWithRegistrations: RegistrationWrapper = {
    val registration = registrationWrapper.registration
    val schemeDetails = registration.schemeDetails
    val updatedSchemeDetails = schemeDetails.copy(
      euRegistrationDetails = List(etmpDisplayEuRegistrationDetails),
      previousEURegistrationDetails = List(etmpEuPreviousRegistrationDetails)
    )

    registrationWrapper.copy(registration = registration.copy(schemeDetails = updatedSchemeDetails))
  }

  "validateEuRegistrations" - {
    "must return success when there are eu registrations and no infractions" in {
      when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
        ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.euRegistrationDetails)
      )(any(), any(), any()))
        .thenReturn(Future.successful(Right(true)))

      when(mockEuRegistrationsValidationService.validatePreviousEuRegistrationDetails(
        ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.previousEURegistrationDetails)
      )(any(), any(), any()))
        .thenReturn(Future.successful(Right(true)))

      val errorRedirectOrSuccess = rejoinRegistrationValidation.validateEuRegistrations(
        registrationWrapperWithRegistrations,
        rejoinWaypoints
      ).futureValue

      errorRedirectOrSuccess mustBe Right(true)
    }

    "must remap invalid infractions on eu registrations" - {
      "for a found ActiveTrader" in {
        val countryCode = arbitraryCountry.arbitrary.sample.value.code
        val memberState = "memberState"

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.euRegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(InvalidActiveTrader(countryCode, memberState))))

        val errorRedirectOrSuccess = rejoinRegistrationValidation.validateEuRegistrations(
          registrationWrapperWithRegistrations,
          rejoinWaypoints
        ).futureValue

        errorRedirectOrSuccess mustBe Left(FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(
          rejoinWaypoints,
          countryCode
        ))
      }

      "for a found QuarantinedTrader" in {
        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.euRegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(InvalidQuarantinedTrader)))

        val errorRedirectOrSuccess = rejoinRegistrationValidation.validateEuRegistrations(
          registrationWrapperWithRegistrations,
          rejoinWaypoints
        ).futureValue

        errorRedirectOrSuccess mustBe Left(ExcludedVRNController.onPageLoad())
      }
    }

    "must remap invalid infractions on previous eu registrations" - {
      "for a found ActiveTrader" in {
        val countryCode = arbitraryCountry.arbitrary.sample.value.code
        val memberState = "memberState"

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.euRegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Right(true)))

        when(mockEuRegistrationsValidationService.validatePreviousEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.previousEURegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(InvalidActiveTrader(countryCode, memberState))))

        val errorRedirectOrSuccess = rejoinRegistrationValidation.validateEuRegistrations(
          registrationWrapperWithRegistrations,
          rejoinWaypoints
        ).futureValue

        errorRedirectOrSuccess mustBe Left(SchemeStillActiveController.onPageLoad(EmptyWaypoints, countryCode))
      }

      "for a found QuarantinedTrader" in {
        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.euRegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Right(true)))

        when(mockEuRegistrationsValidationService.validatePreviousEuRegistrationDetails(
          ArgumentMatchers.eq(registrationWrapperWithRegistrations.registration.schemeDetails.previousEURegistrationDetails)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(InvalidQuarantinedTrader)))

        val errorRedirectOrSuccess = rejoinRegistrationValidation.validateEuRegistrations(
          registrationWrapperWithRegistrations,
          rejoinWaypoints
        ).futureValue

        errorRedirectOrSuccess mustBe Left(SchemeQuarantinedController.onPageLoad())
      }
    }
  }
}
