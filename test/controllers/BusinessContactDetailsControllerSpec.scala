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
import config.FrontendAppConfig
import connectors.RegistrationConnector
import forms.BusinessContactDetailsFormProvider
import models.{BankDetails, BusinessContactDetails, CheckMode}
import models.emailVerification.EmailVerificationResponse
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.{BankDetailsPage, BusinessContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.inject.bind
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.{EmailVerificationService, SaveForLaterService}
import utils.FutureSyntax.FutureOps
import views.html.BusinessContactDetailsView

class BusinessContactDetailsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new BusinessContactDetailsFormProvider()
  private val form = formProvider()

  private lazy val businessContactDetailsRoute = routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints).url
  private lazy val amendBusinessContactDetailsRoute = routes.BusinessContactDetailsController.onPageLoad(amendWaypoints).url
  private lazy val amendPreviousBusinessContactDetailsRoute = routes.BusinessContactDetailsController.onPageLoad(amendPreviousWaypoints).url

  private val userAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

  private val emptyWaypoints = EmptyWaypoints
  private val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val amendPreviousWaypoints =
    EmptyWaypoints.setNextWaypoint(Waypoint(ChangePreviousRegistrationPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))

  private val mockEmailVerificationService = mock[EmailVerificationService]
  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockSaveForLaterService = mock[SaveForLaterService]

  private def createEmailVerificationResponse(waypoints: Waypoints): EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = routes.BankDetailsController.onPageLoad(waypoints).url
  )

  private val amendEmailVerificationResponse: EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
  )

  private val bankDetails = BankDetails(
    accountName = "Account name",
    bic = Some(bic),
    iban = iban
  )

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailVerificationService)
    Mockito.reset(mockRegistrationConnector)
  }

  "BusinessContactDetails Controller" - {

    "GET" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .build()

        running(application) {
          val request = FakeRequest(GET, businessContactDetailsRoute)

          val view = application.injector.instanceOf[BusinessContactDetailsView]

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, emptyWaypoints, None, 1)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, businessContactDetailsRoute)

          val view = application.injector.instanceOf[BusinessContactDetailsView]

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(form.fill(contactDetails), emptyWaypoints, None, 1)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET when a previous registration is being amended" in {

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, amendPreviousBusinessContactDetailsRoute)

          val view = application.injector.instanceOf[BusinessContactDetailsView]

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(form, amendPreviousWaypoints, None, 1)(request, messages(application)).toString
        }
      }
    }

    "onSubmit" - {

      "when email verification enabled" - {

        "must save the answer and redirect to the next page if email is already verified and valid data is submitted" in {
          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn Verified.toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.BankDetailsController.onPageLoad(emptyWaypoints).url

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(0))
              .createEmailVerificationRequest(
                eqTo(emptyWaypoints),
                eqTo(emailVerificationRequest.credId),
                eqTo(emailVerificationRequest.email.get.address),
                eqTo(emailVerificationRequest.pageTitle),
                eqTo(emailVerificationRequest.continueUrl))(any())
          }
        }

        "must save the answer and redirect to the Business Contact Details page if email is not verified and valid data is submitted" in {
          val emailVerificationResponse = createEmailVerificationResponse(emptyWaypoints)

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              continueUrl = s"${config.loginContinueUrl}${emailVerificationRequest.continueUrl}"
            )

            status(result) mustBe SEE_OTHER

            redirectLocation(result).value mustBe config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(anEmailVerificationRequest.email.value.address), eqTo(anEmailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must save the answer and redirect to the Bank Details page if if bank details are not completed" in {

          val emailVerificationResponse = EmailVerificationResponse(
            redirectUri = routes.BankDetailsController.onPageLoad().url
          )

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              continueUrl = s"${config.loginContinueUrl}${emailVerificationRequest.continueUrl}"
            )

            status(result) mustBe SEE_OTHER

            redirectLocation(result).value mustBe config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.value.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must redirect to the CheckYourAnswersPage if bank details are completed" in {
          val emailVerificationResponse = EmailVerificationResponse(
            redirectUri = routes.CheckYourAnswersController.onPageLoad().url
          )

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = completeUserAnswersWithVatInfo
              .set(BusinessContactDetailsPage, contactDetails).success.value
              .set(BankDetailsPage, bankDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              continueUrl = s"${config.loginContinueUrl}/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop/check-your-answers"
            )

            status(result) mustBe SEE_OTHER

            redirectLocation(result).value mustBe config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.value.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must save the answer and redirect to the Email Verification Codes Exceeded page if valid data is submitted but" +
          " verification attempts on a single email are exceeded" in {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

          when(mockSaveForLaterService.saveAnswers(any(), any(), any())(any(), any(), any())) thenReturn
            Redirect(routes.EmailVerificationCodesExceededController.onPageLoad()).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
              .build()

          running(application) {

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(
                  ("fullName", "name"),
                  ("telephoneNumber", "0111 2223334"),
                  ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) mustBe SEE_OTHER

            val expected: String = routes.EmailVerificationCodesExceededController.onPageLoad().url
            val actual: String = redirectLocation(result).value

            actual mustBe expected

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verifyNoMoreInteractions(mockEmailVerificationService)

            verify(mockSaveForLaterService, times(1))
              .saveAnswers(
                eqTo(emptyWaypoints),
                eqTo(routes.EmailVerificationCodesExceededController.onPageLoad()),
                eqTo(routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints))
              )(any(), any(), any())
          }
        }

        "must save the answer and redirect to the Email Verification Codes and Emails Exceeded page if valid data is submitted but" +
          " verification attempts on maximum emails are exceeded" in {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn LockedTooManyLockedEmails.toFuture

          when(mockSaveForLaterService.saveAnswers(any(), any(), any())(any(), any(), any())) thenReturn
            Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[SaveForLaterService].toInstance(mockSaveForLaterService)
              ).build()

          running(application) {

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verifyNoMoreInteractions(mockEmailVerificationService)

            verify(mockSaveForLaterService, times(1))
              .saveAnswers(
                eqTo(emptyWaypoints),
                eqTo(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()),
                eqTo(routes.BusinessContactDetailsController.onPageLoad())
              )(any(), any(), any())
          }
        }

        "must not save the answer and redirect to the current page when invalid email is submitted" in {

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          val httpStatus = Gen.oneOf(BAD_REQUEST, UNAUTHORIZED, INTERNAL_SERVER_ERROR, BAD_GATEWAY).sample.value

          when(mockSessionRepository.set(any())) thenReturn false.toFuture
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn NotVerified.toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          val config = application.injector.instanceOf[FrontendAppConfig]

          val anEmailVerificationRequest = emailVerificationRequest.copy(
            continueUrl = s"${config.loginContinueUrl}${emailVerificationRequest.continueUrl}"
          )

          when(mockEmailVerificationService.createEmailVerificationRequest(
            eqTo(emptyWaypoints),
            eqTo(anEmailVerificationRequest.credId),
            eqTo(anEmailVerificationRequest.email.get.address),
            eqTo(anEmailVerificationRequest.pageTitle),
            eqTo(anEmailVerificationRequest.continueUrl))(any())) thenReturn
            Left(UnexpectedResponseStatus(httpStatus, "error")).toFuture

          running(application) {

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints).url

            verifyNoInteractions(mockSessionRepository)

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(anEmailVerificationRequest.email.get.address), eqTo(anEmailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                eqTo(emptyWaypoints),
                eqTo(anEmailVerificationRequest.credId),
                eqTo(anEmailVerificationRequest.email.get.address),
                eqTo(anEmailVerificationRequest.pageTitle),
                eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }
      }

      "when email verification disabled" - {

        "must save the answer and redirect to the next page when valid data is submitted" in {

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "false")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.BankDetailsController.onPageLoad().url
            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
            verifyNoInteractions(mockEmailVerificationService)
          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .configure("features.enrolments-enabled" -> "false")
          .build()

        running(application) {
          val request =
            FakeRequest(POST, businessContactDetailsRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[BusinessContactDetailsView]

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe view(boundForm, emptyWaypoints, None, 1)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, businessContactDetailsRoute)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .configure("features.enrolments-enabled" -> "false")
          .build()

        running(application) {
          val request =
            FakeRequest(POST, businessContactDetailsRoute)
              .withFormUrlEncodedBody(("fullName", "value 1"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@email.com"))

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }

  "inAmend" - {

    "must save the answer and redirect to the next page if email is already verified and valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

      when(mockEmailVerificationService.isEmailVerified(
        eqTo(emailVerificationRequest.email.get.address),
        eqTo(emailVerificationRequest.credId))(any())) thenReturn Verified.toFuture

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .configure("features.email-verification-enabled" -> "true")
          .configure("features.enrolments-enabled" -> "false")
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[EmailVerificationService].toInstance(mockEmailVerificationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, amendBusinessContactDetailsRoute)
            .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

        verify(mockEmailVerificationService, times(1))
          .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

        verify(mockEmailVerificationService, times(0))
          .createEmailVerificationRequest(
            eqTo(emptyWaypoints),
            eqTo(emailVerificationRequest.credId),
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.pageTitle),
            eqTo(emailVerificationRequest.continueUrl))(any())
      }

    }

    "must save the answer and redirect to the next page if email is already verified and valid data is submitted for a previous registration" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

      when(mockEmailVerificationService.isEmailVerified(
        eqTo(emailVerificationRequest.email.get.address),
        eqTo(emailVerificationRequest.credId))(any())) thenReturn Verified.toFuture

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .configure("features.email-verification-enabled" -> "true")
          .configure("features.enrolments-enabled" -> "false")
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[EmailVerificationService].toInstance(mockEmailVerificationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, amendPreviousBusinessContactDetailsRoute)
            .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

        verify(mockEmailVerificationService, times(1))
          .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

        verify(mockEmailVerificationService, times(0))
          .createEmailVerificationRequest(
            eqTo(emptyWaypoints),
            eqTo(emailVerificationRequest.credId),
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.pageTitle),
            eqTo(emailVerificationRequest.continueUrl))(any())
      }
    }

    "must save the answer and redirect to the Business Contact Details page if email is not verified and valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

      val newEmailAddress = "email@example.co.uk"

      val amendEmailVerificationRequest = emailVerificationRequest.copy(
        email = emailVerificationRequest.email.map(_.copy(address = newEmailAddress)),
        continueUrl = controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
      )

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockEmailVerificationService.isEmailVerified(
        any(),
        any())(any())) thenReturn NotVerified.toFuture
      when(mockEmailVerificationService.createEmailVerificationRequest(
        any(),
        any(),
        any(),
        any(),
        any())(any())) thenReturn Right(amendEmailVerificationResponse).toFuture

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .configure("features.email-verification-enabled" -> "true")
          .configure("features.enrolments-enabled" -> "false")
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[EmailVerificationService].toInstance(mockEmailVerificationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, amendBusinessContactDetailsRoute)
            .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", newEmailAddress))

        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val expectedAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails.copy(emailAddress = newEmailAddress)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe config.emailVerificationUrl + amendEmailVerificationResponse.redirectUri
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

        verify(mockEmailVerificationService, times(1))
          .isEmailVerified(eqTo(newEmailAddress), eqTo(amendEmailVerificationRequest.credId))(any())

        verify(mockEmailVerificationService, times(0))
          .createEmailVerificationRequest(
            eqTo(emptyWaypoints),
            eqTo(emailVerificationRequest.credId),
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.pageTitle),
            eqTo(emailVerificationRequest.continueUrl))(any())
      }
    }

    "must return OK and the correct view for a GET when Oss Registration" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration)
        .build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val expectedContactDetails = BusinessContactDetails(
          fullName = "Rory Beans",
          telephoneNumber = "01234567890",
          emailAddress = "roryBeans@beans.com"
        )

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(expectedContactDetails), emptyWaypoints, ossRegistration, 0)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Oss Registration and Ioss registrations" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration, numberOfIossRegistrations = 1)
        .build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val expectedContactDetails = BusinessContactDetails(
          fullName = "Rory Beans",
          telephoneNumber = "01234567890",
          emailAddress = "roryBeans@beans.com"
        )

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(expectedContactDetails), emptyWaypoints, ossRegistration, 1)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET 1 previous Ioss registrations" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), numberOfIossRegistrations = 1)
        .build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, emptyWaypoints, None, 1)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET more than 1 Ioss registrations" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), numberOfIossRegistrations = 2)
        .build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, emptyWaypoints, None, 2)(request, messages(application)).toString
      }
    }
  }
}
