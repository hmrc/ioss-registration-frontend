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

package controllers.test

import base.SpecBase
import connectors.test.{DownstreamServiceError, EmailPasscodeEntry, EmailPasscodes, FailedToFetchTestOnlyPasscode, TestOnlyEmailPasscodeConnector}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future


class TestOnlyEmailPasscodeControllerSpec extends SpecBase with MockitoSugar {

  private val testOnlyEmailPasscodeConnector = mock[TestOnlyEmailPasscodeConnector]
  private val testOnlyEmailPasscodeRoute = controllers.test.routes.TestOnlyEmailPasscodeController.testOnlyGetPasscodes().url

  ".testOnlyGetPasscodes" - {

    "must return OK when a passcode is returned successfully" in {

      val application = applicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(bind[TestOnlyEmailPasscodeConnector].toInstance(testOnlyEmailPasscodeConnector))
        .build()

      running(application) {

        val testOnlyResponse = EmailPasscodes(Seq(EmailPasscodeEntry("test@test.com", "passcode")))

        when(testOnlyEmailPasscodeConnector.getTestOnlyPasscode()(any())) thenReturn Future.successful(Right(testOnlyResponse))

        val request = FakeRequest(GET, testOnlyEmailPasscodeRoute)

        val result = route(application, request).value

        status(result) mustBe 200
      }

    }

    "must return Service Error when server responds with an error" in {

      val application = applicationBuilder()
        .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .overrides(bind[TestOnlyEmailPasscodeConnector].toInstance(testOnlyEmailPasscodeConnector))
        .build()

      running(application) {

        when(testOnlyEmailPasscodeConnector.getTestOnlyPasscode()(any())) thenReturn
          Future.successful(Left(DownstreamServiceError(
            "Some exception",
            FailedToFetchTestOnlyPasscode("Failed to get test only passcodes"))))

        val request = FakeRequest(GET, testOnlyEmailPasscodeRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[DownstreamServiceError] }
      }

    }
  }

}
