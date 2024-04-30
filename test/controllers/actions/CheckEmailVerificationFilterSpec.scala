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
import connectors.RegistrationConnector
import controllers.routes
import models.CheckMode
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, EmptyWaypoints, Waypoint}
import pages.amend.ChangeRegistrationPage
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.{EmailVerificationService, SaveForLaterService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEmailVerificationFilterSpec extends SpecBase with MockitoSugar with EitherValues {

  class Harness(
                 inAmend: Boolean,
                 frontendAppConfig: FrontendAppConfig,
                 emailVerificationService: EmailVerificationService,
                 saveForLaterService: SaveForLaterService,
                 registrationConnector: RegistrationConnector
               )
    extends CheckEmailVerificationFilterImpl(inAmend, frontendAppConfig, emailVerificationService, saveForLaterService, registrationConnector) {
    def callFilter(request: AuthenticatedDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockEmailVerificationService = mock[EmailVerificationService]
  private val mockSaveForLaterService = mock[SaveForLaterService]
  private val validEmailAddressUserAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value
  private val mockRegistrationConnector = mock[RegistrationConnector]

  private val expectedWaypoints =
    EmptyWaypoints.setNextWaypoint(
      Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment)
    )

  ".filter" - {

    "when email verification enabled" - {

      "must return None if no email address is present" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, basicUserAnswersWithVatInfo, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "must return None when an email address is verified" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn Verified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "must redirect to Business Contact Details page when an email address is not verified and not in amend" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn NotVerified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(controllers.routes.BusinessContactDetailsController.onPageLoad(expectedWaypoints).url))
        }
      }

      "must redirect to Business Contact Details page when an email address is not verified and in amend" in  {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(
            registrationWrapper.copy(
              registration = registrationWrapper.registration.copy(
                schemeDetails = registrationWrapper.registration.schemeDetails.copy(
                  businessEmailId = "newEmail@domain.com"
                )
              )
            )
          ).toFuture

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn NotVerified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = true, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(controllers.routes.BusinessContactDetailsController.onPageLoad(expectedWaypoints).url))
        }
      }

      "must save the answers and redirect to Email Verification Codes Exceeded page when verification attempts on a single email are exceeded" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

          when(mockSaveForLaterService.saveAnswersRedirect(any(), any())(any(), any(), any())) thenReturn
            Redirect(routes.EmailVerificationCodesExceededController.onPageLoad()).toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url))

          verify(mockSaveForLaterService, times(1))
            .saveAnswersRedirect(
              eqTo(routes.EmailVerificationCodesExceededController.onPageLoad().url),
              eqTo(request.uri)
            )(any(), any(), any())
        }
      }

      "must redirect to Email Verification Codes and Emails Exceeded page when verification attempts on maximum email addresses are exceeded" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn LockedTooManyLockedEmails.toFuture


          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url))
        }
      }
    }

    "when email verification disabled" - {

      "must return None if no email address is present" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "false")
          .build()

        running(app) {

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, basicUserAnswersWithVatInfo, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = false, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }
    }

    "when inAmend is true" - {

      "must return None if no email address is present" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, basicUserAnswersWithVatInfo, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = true, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "must return None when an email address is verified" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn Verified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = true, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "must return None when an email address in userAnswers matches the one in the registration" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn Verified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, None, validEmailAddressUserAnswers, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
          val controller = new Harness(inAmend = true, frontendAppConfig, mockEmailVerificationService, mockSaveForLaterService, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }
    }
  }
}
