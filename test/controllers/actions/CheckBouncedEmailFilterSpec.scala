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
import config.FrontendAppConfig
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.{BusinessContactDetails, CheckMode}
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, EmptyWaypoints, Waypoint}
import pages.amend.ChangeRegistrationPage
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.EmailVerificationService
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.inject.bind
import uk.gov.hmrc.auth.core.Enrolments

class CheckBouncedEmailFilterSpec extends SpecBase with MockitoSugar {

  class Harness(
                 frontendAppConfig: FrontendAppConfig,
                 emailVerificationService: EmailVerificationService
               )
    extends CheckBouncedEmailFilterImpl(frontendAppConfig, emailVerificationService) {
    def callFilter(request: AuthenticatedMandatoryIossRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val authDataRequest = AuthenticatedDataRequest(
    FakeRequest(),
    testCredentials,
    vrn,
    Enrolments(Set.empty),
    Some(iossNumber),
    completeUserAnswers,
    Some(registrationWrapper),
    1,
    None
  )

  ".filter" - {

    "when the unusable email status is true" - {

      "and email is the same as answers" - {

        "and email address is not verified" - {

          "must redirect to Intercept Unusable Email" in {

            val app = applicationBuilder(None)
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            val testEmail = "test@test.com"

            val userAnswers = completeUserAnswers.set(BusinessContactDetailsPage, BusinessContactDetails("test name", "123456", testEmail)).success.value

            val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
              registrationWrapper.registration.copy(schemeDetails =
                registrationWrapper.registration.schemeDetails.copy(unusableStatus = true, businessEmailId = testEmail)))

            val changeRegWaypoint = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

            running(app) {

              when(mockEmailVerificationService.isEmailVerified(
                eqTo(testEmail), eqTo(userAnswersId))(any())) thenReturn NotVerified.toFuture

              val request = AuthenticatedMandatoryIossRequest(
                authDataRequest,
                testCredentials,
                vrn,
                Enrolments(Set.empty),
                iossNumber,
                regWrapperWithUnusableEmail,
                userAnswers,
                1,
                None)
              val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
              val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

              val result = controller.callFilter(request).futureValue

              result.value mustEqual Redirect(controllers.routes.BusinessContactDetailsController.onPageLoad(changeRegWaypoint))

            }
          }
        }

        "and verification attempts on maximum email addresses are exceeded" - {

          "must redirect to Email Verification Codes and Emails Exceeded page" in {

            val app = applicationBuilder(None)
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            val testEmail = "test@test.com"

            val userAnswers = completeUserAnswers.set(BusinessContactDetailsPage, BusinessContactDetails("test name", "123456", testEmail)).success.value

            val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
              registrationWrapper.registration.copy(schemeDetails =
                registrationWrapper.registration.schemeDetails.copy(unusableStatus = true, businessEmailId = testEmail)))

            running(app) {

              when(mockEmailVerificationService.isEmailVerified(
                eqTo(testEmail), eqTo(userAnswersId))(any())) thenReturn LockedTooManyLockedEmails.toFuture

              val request = AuthenticatedMandatoryIossRequest(
                authDataRequest,
                testCredentials,
                vrn,
                Enrolments(Set.empty),
                iossNumber,
                regWrapperWithUnusableEmail,
                userAnswers,
                1,
                None
              )
              val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
              val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

              val result = controller.callFilter(request).futureValue

              result.value mustEqual Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)
            }
          }
        }

        "and verification attempts on a single email are exceeded" - {

          "must redirect to Email Verification Codes Exceeded page" in {

            val app = applicationBuilder(None)
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            val testEmail = "test@test.com"

            val userAnswers = completeUserAnswers.set(BusinessContactDetailsPage, BusinessContactDetails("test name", "123456", testEmail)).success.value

            val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
              registrationWrapper.registration.copy(schemeDetails =
                registrationWrapper.registration.schemeDetails.copy(unusableStatus = true, businessEmailId = testEmail)))

            running(app) {

              when(mockEmailVerificationService.isEmailVerified(
                eqTo(testEmail), eqTo(userAnswersId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

              val request = AuthenticatedMandatoryIossRequest(
                authDataRequest,
                testCredentials,
                vrn,
                Enrolments(Set.empty),
                iossNumber,
                regWrapperWithUnusableEmail,
                userAnswers,
                1,
                None
              )
              val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
              val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

              val result = controller.callFilter(request).futureValue

              result.value mustEqual Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url)
            }
          }
        }

        "and email address is verified" - {

          "must be None" in {

            val app = applicationBuilder(None)
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            val testEmail = "test@test.com"

            val userAnswers = completeUserAnswers.set(BusinessContactDetailsPage, BusinessContactDetails("test name", "123456", testEmail)).success.value

            val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
              registrationWrapper.registration.copy(schemeDetails =
                registrationWrapper.registration.schemeDetails.copy(unusableStatus = true, businessEmailId = testEmail)))

            running(app) {

              when(mockEmailVerificationService.isEmailVerified(
                eqTo(testEmail), eqTo(userAnswersId))(any())) thenReturn Verified.toFuture

              val request = AuthenticatedMandatoryIossRequest(
                authDataRequest,
                testCredentials,
                vrn,
                Enrolments(Set.empty),
                iossNumber,
                regWrapperWithUnusableEmail,
                userAnswers,
                1,
                None
              )
              val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
              val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

              val result = controller.callFilter(request).futureValue

              result mustBe None
            }
          }
        }
      }

      "and email has been updated" - {
        "must be None" in {

          val app = applicationBuilder(None)
            .build()

          val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
            registrationWrapper.registration.copy(schemeDetails =
              registrationWrapper.registration.schemeDetails.copy(unusableStatus = true)))

          val updatedUserAnswers = completeUserAnswersWithVatInfo.set(BusinessContactDetailsPage, BusinessContactDetails(
            fullName = registrationWrapper.registration.schemeDetails.contactName,
            telephoneNumber = registrationWrapper.registration.schemeDetails.businessTelephoneNumber,
            emailAddress = s"1${registrationWrapper.registration.schemeDetails.businessEmailId}",
          )).success.value

          running(app) {
            val request = AuthenticatedMandatoryIossRequest(
              authDataRequest,
              testCredentials,
              vrn,
              Enrolments(Set.empty),
              iossNumber,
              regWrapperWithUnusableEmail,
              updatedUserAnswers,
              1,
              None
            )
            val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
            val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

            val result = controller.callFilter(request).futureValue

            result mustBe None

          }
        }
      }
    }

    "when the unusable email status is false" - {

      "must be None" in {

        val app = applicationBuilder(None)
          .build()

        running(app) {
          val request = AuthenticatedMandatoryIossRequest(
            authDataRequest, testCredentials, vrn, Enrolments(Set.empty), iossNumber, registrationWrapper, completeUserAnswersWithVatInfo, 1, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(frontendAppConfig, mockEmailVerificationService)

          val result = controller.callFilter(request).futureValue

          result mustBe None

        }
      }

    }

  }
}
