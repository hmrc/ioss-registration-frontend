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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.emailVerification.{EmailStatus, EmailVerificationResponse, VerificationStatus}
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.EmptyWaypoints
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val config = mock[FrontendAppConfig]
  private val mockEmailVerificationConnector = mock[EmailVerificationConnector]
  private val emailVerificationService = new EmailVerificationService(config, mockEmailVerificationConnector)

  private val journeyId = "98fe3788-2d39-409c-b400-8f86ed1634ea"
  private val emailVerificationResponse = EmailVerificationResponse(redirectUri = journeyId)


  ".createEmailVerificationRequest" - {

    "must return a Right of Email Verification Response when connector verifyEmail method called with valid payload" in {

      val continueUrl: String = "/continueUrl"

      when(config.origin) thenReturn "OSS"
      when(mockEmailVerificationConnector.verifyEmail(any())(any(), any())) thenReturn Future.successful(Right(emailVerificationResponse))

      val result = emailVerificationService.createEmailVerificationRequest(
        EmptyWaypoints,
        userAnswersId,
        contactDetails.emailAddress,
        Some("Page title"),
        continueUrl
      )

      result.futureValue mustBe Right(emailVerificationResponse)
    }
  }

  ".getStatus" - {

    "must return Right verification status when an email verification request exists" in {

      val verifiedEmail = EmailStatus(emailAddress = contactDetails.emailAddress, verified = true, locked = false)
      val verificationStatus: VerificationStatus = VerificationStatus(Seq(verifiedEmail))

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.getStatus(userAnswersId).futureValue

      result mustBe Right(Some(verificationStatus))
    }

    "must return Left UnexpectedResponseStatus when an error occurs " in {

      val errorCode = Gen.oneOf(BAD_REQUEST, UNAUTHORIZED, INTERNAL_SERVER_ERROR, BAD_GATEWAY).sample.value

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn
        Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val result = emailVerificationService.getStatus(userAnswersId).futureValue

      result mustBe Left(UnexpectedResponseStatus(errorCode, "error"))
    }
  }

  ".isEmailVerified" - {

    "must return Verified if email is verified" in {

      val verifiedEmail: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = true, locked = false)
      val verificationStatus: VerificationStatus = VerificationStatus(Seq(verifiedEmail))

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe Verified

    }

    "must return Verified if email is verified and there are other locked emails" in {

      val lockedEmail: EmailStatus = EmailStatus(emailAddress = "test@test.co.uk", verified = false, locked = true)
      val verifiedEmail: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = true, locked = false)
      val verificationStatus: VerificationStatus = VerificationStatus(Seq(lockedEmail, verifiedEmail))

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe Verified

    }

    "must return Not Verified if email is not verified" in {

      val unverifiedEmail: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = false, locked = false)
      val verificationStatus: VerificationStatus = VerificationStatus(Seq(unverifiedEmail))

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe NotVerified

    }

    "must return Not Verified if there is no verification status received" in {

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(None))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe NotVerified

    }

    "must return Locked Passcode For Single Email if email is not verified and locked for a single email" in {

      val locked: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = false, locked = true)
      val verificationStatus: VerificationStatus = VerificationStatus(Seq(locked))

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe LockedPasscodeForSingleEmail

    }

    "must return Locked Passcode For Single Email if email is not verified and locked for a single email with multiple email address attempts" in {

      val locked: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = false, locked = true)
      val verificationStatus: VerificationStatus = VerificationStatus(
        Seq(locked, locked, locked, locked, locked, locked, locked, locked, locked)
      )

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe LockedPasscodeForSingleEmail

    }

    "must return Locked Too Many Locked Emails if if email is not verified and locked due to maximum email address attempts" in {

      val locked: EmailStatus = EmailStatus(emailAddress = contactDetails.emailAddress, verified = false, locked = true)
      val verificationStatus: VerificationStatus = VerificationStatus(
        Seq(locked, locked, locked, locked, locked, locked, locked, locked, locked, locked)
      )

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Right(Some(verificationStatus)))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe LockedTooManyLockedEmails

    }

    "must return Not Verified for a Left(UnexpectedResponseStatus)" in {

      when(mockEmailVerificationConnector.getStatus(userAnswersId)) thenReturn Future.successful(Left(UnexpectedResponseStatus(BAD_REQUEST, "error")))

      val result = emailVerificationService.isEmailVerified(contactDetails.emailAddress, userAnswersId).futureValue

      result mustBe NotVerified

    }

  }

}


