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
import models.amend.RegistrationWrapper
import models.external.ExternalEntryUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testutils.RegistrationData.etmpDisplayRegistration
import views.html.AlreadyRegisteredView

import scala.concurrent.Future

class AlreadyRegisteredControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val registrationWrapper:RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "AlreadyRegistered Controller" - {

    "when the connector returns a registration" - {

      "must return OK and the correct view for a GET" in {

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Future.successful(Right(registrationWrapper))
        when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.AlreadyRegisteredController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AlreadyRegisteredView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          val expectedContent =
            view(
              config.feedbackUrl(request)
            )(request, messages(application)).toString

          contentAsString(result) mustEqual expectedContent
        }

      }
    }

    "when the connector does not find an existing registration" - {

      "must redirect the user to the Problem With Account page" in {

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Future.successful(Right(registrationWrapper))
        when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.AlreadyRegisteredController.onPageLoad().url)

          val result = route(application, request).value

          if (registrationWrapper == null) {
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.ProblemWithAccountController.onPageLoad().url
          }
        }
      }
    }
  }

}
