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
import models.CheckMode
import models.emailVerification.EmailVerificationResponse
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{BusinessContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.http.Status.BAD_REQUEST
import play.api.inject.bind
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.{EmailVerificationService, SaveForLaterService}
import views.html.BusinessContactDetailsView

import scala.concurrent.Future

class BusinessContactDetailsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new BusinessContactDetailsFormProvider()
  private val form = formProvider()

  private lazy val businessContactDetailsRoute = routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints).url
  private lazy val amendBusinessContactDetailsRoute = routes.BusinessContactDetailsController.onPageLoad(amendWaypoints).url

  private val userAnswers = basicUserAnswersWithVatInfo.set(BusinessContactDetailsPage, contactDetails).success.value

  private val emptyWaypoints = EmptyWaypoints
  private val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val mockEmailVerificationService = mock[EmailVerificationService]
  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockSaveForLaterService = mock[SaveForLaterService]

  private def createEmailVerificationResponse(waypoints: Waypoints): EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = routes.BankDetailsController.onPageLoad(waypoints).url
  )

  private val amendEmailVerificationResponse: EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
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

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, businessContactDetailsRoute)

          val view = application.injector.instanceOf[BusinessContactDetailsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(contactDetails), emptyWaypoints)(request, messages(application)).toString
        }
      }
    }

    "onSubmit" - {

      "when email verification enabled" - {
        "must save the answer and redirect to the next page if email is already verified and valid data is submitted" in {
          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn Future.successful(Verified)

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

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.BankDetailsController.onPageLoad(emptyWaypoints).url
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

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn Future.successful(NotVerified)

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Future.successful(Right(emailVerificationResponse))

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

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.value.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(emailVerificationRequest.credId),
                emailAddress = eqTo(emailVerificationRequest.email.value.address),
                pageTitle = eqTo(emailVerificationRequest.pageTitle),
                continueUrl = eqTo(emailVerificationRequest.continueUrl))(any())

          }
        }

        "must save the answer and redirect to the Email Verification Codes Exceeded page if valid data is submitted but" +
          " verification attempts on a single email are exceeded" in {

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[SaveForLaterService].toInstance(mockSaveForLaterService)
              )
              .build()

          running(application) {
            when(mockEmailVerificationService.isEmailVerified(
              eqTo(emailVerificationRequest.email.get.address),
              eqTo(emailVerificationRequest.credId))(any())) thenReturn Future.successful(LockedPasscodeForSingleEmail)

            when(mockSaveForLaterService.saveAnswers(any(), any())(any(), any(), any())) thenReturn
              Future.successful(Redirect(routes.EmailVerificationCodesExceededController.onPageLoad()))

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(
                  ("fullName", "name"),
                  ("telephoneNumber", "0111 2223334"),
                  ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            val expected: String = routes.EmailVerificationCodesExceededController.onPageLoad().url
            val actual: String = redirectLocation(result).value

            actual mustBe expected

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verifyNoMoreInteractions(mockEmailVerificationService)

            verify(mockSaveForLaterService, times(1))
              .saveAnswers(
                eqTo(routes.EmailVerificationCodesExceededController.onPageLoad()),
                eqTo(routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints))
              )(any(), any(), any())
          }
        }

        "must save the answer and redirect to the Email Verification Codes and Emails Exceeded page if valid data is submitted but" +
          " verification attempts on maximum emails are exceeded" in {

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[SaveForLaterService].toInstance(mockSaveForLaterService)
              )
              .build()

          running(application) {
            when(mockEmailVerificationService.isEmailVerified(
              eqTo(emailVerificationRequest.email.get.address),
              eqTo(emailVerificationRequest.credId))(any())) thenReturn Future.successful(LockedTooManyLockedEmails)

            when(mockSaveForLaterService.saveAnswers(any(), any())(any(), any(), any())) thenReturn
              Future.successful(Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()))

            val request =
              FakeRequest(POST, businessContactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url
            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())
            verifyNoMoreInteractions(mockEmailVerificationService)
            verify(mockSaveForLaterService, times(1))
              .saveAnswers(
                eqTo(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()),
                eqTo(routes.BusinessContactDetailsController.onPageLoad())
              )(any(), any(), any())
          }
        }

        "must not save the answer and redirect to the current page when invalid email is submitted" in {


          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          val httpStatus = Gen.oneOf(BAD_REQUEST, UNAUTHORIZED, INTERNAL_SERVER_ERROR, BAD_GATEWAY).sample.value

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn Future.successful(NotVerified)
          when(mockEmailVerificationService.createEmailVerificationRequest(
            eqTo(emptyWaypoints),
            eqTo(emailVerificationRequest.credId),
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.pageTitle),
            eqTo(emailVerificationRequest.continueUrl))(any())) thenReturn
            Future.successful(Left(UnexpectedResponseStatus(httpStatus, "error")))

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

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.BusinessContactDetailsController.onPageLoad(emptyWaypoints).url
            verifyNoInteractions(mockSessionRepository)
            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())
            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                eqTo(emptyWaypoints),
                eqTo(emailVerificationRequest.credId),
                eqTo(emailVerificationRequest.email.get.address),
                eqTo(emailVerificationRequest.pageTitle),
                eqTo(emailVerificationRequest.continueUrl))(any())
          }
        }
      }

      "when email verification disabled" - {
        "must save the answer and redirect to the next page when valid data is submitted" in {

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

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

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.BankDetailsController.onPageLoad().url
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

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, businessContactDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }

  "inAmend" - {

    "must save the answer and redirect to the next page if email is already verified and valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockRegistrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

      when(mockEmailVerificationService.isEmailVerified(
        eqTo(emailVerificationRequest.email.get.address),
        eqTo(emailVerificationRequest.credId))(any())) thenReturn Future.successful(Verified)

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

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
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
      val mockRegistrationConnector = mock[RegistrationConnector]

      when(mockRegistrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

      val newEmailAddress = "email@example.co.uk"

      val amendEmailVerificationRequest = emailVerificationRequest.copy(
        email = emailVerificationRequest.email.map(_.copy(address = newEmailAddress)),
        continueUrl = controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
      )

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockEmailVerificationService.isEmailVerified(
        any(),
        any())(any())) thenReturn Future.successful(NotVerified)
      when(mockEmailVerificationService.createEmailVerificationRequest(
        any(),
        any(),
        any(),
        any(),
        any())(any())) thenReturn Future.successful(Right(amendEmailVerificationResponse))

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

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.emailVerificationUrl + amendEmailVerificationResponse.redirectUri
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

    // TODO -> With previous registrations
  }

}
