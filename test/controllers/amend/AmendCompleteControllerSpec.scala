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

package controllers.amend

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.amend.{routes => amendRoutes}
import controllers.routes
import models.UserAnswers
import models.external.ExternalEntryUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessContactDetailsPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.amend.AmendCompleteView

import scala.concurrent.Future

class AmendCompleteControllerSpec extends SpecBase with MockitoSugar {

  private val mockRegistrationConnector = mock[RegistrationConnector]

  private  val userAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      BusinessContactDetailsPage.toString -> Json.obj(
        "fullName" -> "value 1",
        "telephoneNumber" -> "value 2",
        "emailAddress" -> "test@test.com",
        "websiteAddress" -> "value 4",
      )
    ),
    vatInfo = Some(vatCustomerInfo)
  )

  "AmendComplete Controller" - {

    "when the scheme has started" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            vrn,
            config.feedbackUrl(request),
            None,
            yourAccountUrl,
            "Company name",
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery and the correct view for a GET with no user answers" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
