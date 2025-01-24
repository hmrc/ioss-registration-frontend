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
import connectors.RegistrationConnector
import models.external.ExternalEntryUrl
import models.responses.{ErrorResponse, InternalServerError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps
import views.html.ErrorSubmittingRegistrationView

class ErrorSubmittingRegistrationControllerSpec extends SpecBase {

  private val externalUrl = "/test-external-url"
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  "ErrorSubmittingRegistration Controller" - {

    "must return OK and the correct view for a GET when an external URL is present" in {

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some(externalUrl))).toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ErrorSubmittingRegistrationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ErrorSubmittingRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(externalUrl))(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when an external URL is not present" in {

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ErrorSubmittingRegistrationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ErrorSubmittingRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the external URL retrieval fails" in {

      val errorResponse: ErrorResponse = InternalServerError

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Left(errorResponse).toFuture

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ErrorSubmittingRegistrationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ErrorSubmittingRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None)(request, messages(application)).toString
      }
    }
  }
}
