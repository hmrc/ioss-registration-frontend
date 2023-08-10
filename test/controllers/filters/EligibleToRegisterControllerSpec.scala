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

package controllers.filters

import base.SpecBase
import controllers.routes
import controllers.filters.{routes => filterRoutes}
import pages.{EmptyWaypoints, Waypoints}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.filters.EligibleToRegisterView

class EligibleToRegisterControllerSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints

  "EligibleToRegister Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, filterRoutes.EligibleToRegisterController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EligibleToRegisterView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the next page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, filterRoutes.EligibleToRegisterController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        //TODO Redirect to auth onSignIn() when created
        redirectLocation(result).value mustBe routes.IndexController.onPageLoad.url
      }
    }
  }
}
