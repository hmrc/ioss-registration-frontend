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

package controllers.actions

import base.SpecBase
import models.core.{Match, MatchType}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckOtherCountryRegistrationFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    "333333333",
    None,
    "DE",
    Some(2),
    None,
    None,
    None,
    None
  )

  override def beforeEach(): Unit = {
    reset(mockCoreRegistrationValidationService)
  }

  class Harness(registrationModificationMode: RegistrationModificationMode, service: CoreRegistrationValidationService) extends
    CheckOtherCountryRegistrationFilterImpl(registrationModificationMode, service) {
    def callFilter(request: AuthenticatedDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  ".filter" - {

    Seq(AmendingActiveRegistration, RejoiningRegistration, NotModifyingExistingRegistration).foreach {
      registrationModificationMode =>

        s"when inAmend is $registrationModificationMode" - {

          "when matchType is FixedEstablishmentActiveNETP" - {

            "must redirect to AlreadyRegisteredOtherCountry page when the user is registered in another OSS service" in {

              val vrn = Vrn("333333331")
              val app = applicationBuilder(None)
                .configure(
                  "features.other-country-reg-validation-enabled" -> true
                )
                .overrides(
                  bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
                ).build()

              running(app) {

                when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(genericMatch))

                val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

                val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

                val result = controller.callFilter(request).futureValue

                if (registrationModificationMode != AmendingActiveRegistration) {
                  result mustBe Some(Redirect(controllers.filters.routes.AlreadyRegisteredOtherCountryController.onPageLoad(genericMatch.memberState).url))
                } else {
                  result mustBe None
                }
                verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
              }
            }
          }

          "when matchType is OtherMSNETPActiveNETP" - {

            "must redirect to AlreadyRegisteredOtherCountry page when the user is registered in another OSS service" in {

              val vrn = Vrn("333333331")
              val app = applicationBuilder(None)
                .configure(
                  "features.other-country-reg-validation-enabled" -> true
                )
                .overrides(
                  bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
                ).build()

              running(app) {

                val expectedMatch = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)

                when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

                val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

                val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

                val result = controller.callFilter(request).futureValue

                if (registrationModificationMode != AmendingActiveRegistration) {
                  result mustBe Some(Redirect(controllers.filters.routes.AlreadyRegisteredOtherCountryController.onPageLoad(expectedMatch.memberState).url))
                } else {
                  result mustBe None
                }
                verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
              }
            }
          }

          "when matchType = OtherMSNETPQuarantinedNETP" - {

            "must redirect to OtherCountryExcludedAndQuarantinedController page when the user is excluded and quarantined from OSS" in {

              val vrn = Vrn("333333331")
              val app = applicationBuilder(None)
                .configure(
                  "features.other-country-reg-validation-enabled" -> true
                )
                .overrides(
                  bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
                ).build()

              running(app) {

                val expectedMatch = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP, exclusionEffectiveDate = Some(LocalDate.of(2022, 10, 10).toString))
                when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

                val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

                val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

                val result = controller.callFilter(request).futureValue

                if (registrationModificationMode != AmendingActiveRegistration) {
                  result mustBe Some(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                    expectedMatch.memberState, expectedMatch.exclusionEffectiveDate.get).url))
                } else {
                  result mustBe None
                }
                verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
              }
            }
          }

          "when matchType = FixedEstablishmentQuarantinedNETP" - {

            "must redirect to OtherCountryExcludedAndQuarantinedController page when the user is excluded and quarantined from OSS" in {

              val vrn = Vrn("333333331")
              val app = applicationBuilder(None)
                .configure(
                  "features.other-country-reg-validation-enabled" -> true
                )
                .overrides(
                  bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
                ).build()

              running(app) {

                val expectedMatch = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP, exclusionEffectiveDate = Some(LocalDate.of(2022, 10, 10).toString))
                when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

                val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

                val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

                val result = controller.callFilter(request).futureValue

                if (registrationModificationMode != AmendingActiveRegistration) {
                  result mustBe Some(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                    expectedMatch.memberState, expectedMatch.exclusionEffectiveDate.get).url))
                } else {
                  result mustBe None
                }
                verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
              }
            }
          }

          "when any matchType and exclusion code is 4" - {

            "must redirect to OtherCountryExcludedAndQuarantinedController page when the user is excluded and quarantined from OSS" in {

              val vrn = Vrn("333333331")
              val app = applicationBuilder(None)
                .configure(
                  "features.other-country-reg-validation-enabled" -> true
                )
                .overrides(
                  bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
                ).build()

              running(app) {

                val expectedMatch = genericMatch.copy(matchType = MatchType.TransferringMSID,
                  exclusionEffectiveDate = Some(LocalDate.of(2022, 10, 10).toString), exclusionStatusCode = Some(4))
                when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

                val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

                val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

                val result = controller.callFilter(request).futureValue

                if (registrationModificationMode != AmendingActiveRegistration) {
                  result mustBe Some(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                    expectedMatch.memberState, expectedMatch.exclusionEffectiveDate.get).url))
                } else {
                  result mustBe None
                }
                verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
              }
            }
          }

          "must return none when the user is not registered in another OSS service" in {

            val vrn = Vrn("333333331")
            val app = applicationBuilder(None)
              .configure(
                "features.other-country-reg-validation-enabled" -> true
              )
              .overrides(
                bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
              ).build()

            running(app) {

              when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(None)

              val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

              val controller = new Harness(registrationModificationMode, mockCoreRegistrationValidationService)

              val result = controller.callFilter(request).futureValue

              result mustBe None
              verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
            }
          }
        }
    }

    "when there is no exclusion effective date" - {

      "must throw an illegal state exception" in {

        val timeout = 30

        val vrn = Vrn("333333331")
        val app = applicationBuilder(None)
          .configure(
            "features.other-country-reg-validation-enabled" -> true
          )
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        running(app) {

          val expectedMatch = genericMatch.copy(matchType = MatchType.TransferringMSID,
            exclusionEffectiveDate = None, exclusionStatusCode = Some(4))
          when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(expectedMatch))

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None)

          val controller = new Harness(registrationModificationMode = NotModifyingExistingRegistration, mockCoreRegistrationValidationService)

          val result = controller.callFilter(request).failed

          whenReady(result, Timeout(Span(timeout, Seconds))) { exp =>
            exp mustBe a[IllegalStateException]
            exp.getMessage must include(s"MatchType ${expectedMatch.matchType} didn't include an expected exclusion effective date")
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
          }
        }
      }
    }
  }
}
