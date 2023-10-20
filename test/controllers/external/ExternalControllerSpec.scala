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

package controllers.external

import base.SpecBase
import controllers.routes
import models.external.{ExternalRequest, ExternalResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.inject
import play.api.inject.bind
import play.api.libs.json.{JsNull, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.external.ExternalService

import scala.concurrent.Future

class ExternalControllerSpec extends SpecBase {
  private val externalRequest = ExternalRequest("BTA", "exampleurl")


  ".onExternal" - {

    "when correct ExternalRequest is posted" - {
      "must respond with OK(IndexController.onPageLoad().url)" in {
        val mockExternalService = mock[ExternalService]

        when(mockExternalService.getExternalResponse(any(), any())) thenReturn
          Future.successful(ExternalResponse(controllers.routes.IndexController.onPageLoad().url))

        val application = applicationBuilder()
          .overrides(inject.bind[ExternalService].toInstance(mockExternalService))
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.external.routes.ExternalController.onExternal().url).withJsonBody(
            Json.toJson(externalRequest)
          )

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(controllers.routes.IndexController.onPageLoad().url)
        }
      }

    }



    "must respond with BadRequest" - {
      "when no body provided" in {
        val application = applicationBuilder()
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.external.routes.ExternalController.onExternal().url).withJsonBody(JsNull)

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }

      "when malformed body provided" in {
        val application = applicationBuilder()
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ExternalController.onExternal().url).withJsonBody(Json.toJson("wrong body"))

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }
    }

  }
}