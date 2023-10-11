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

package controllers.previousRegistrations

import base.SpecBase
import models.{Country, Index}
import pages.{EmptyWaypoints, Waypoints}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.previousRegistrations.SchemeStillActiveView

class SchemeStillActiveControllerSpec extends SpecBase {

  private val index: Index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints

  "SchemeStillActiveController Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET in NormalMode" in {

        val country = Country.getCountryName("EE")

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(waypoints, "EE", index, index).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeStillActiveView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country, index, index)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET in AmendMode" in {

        val country = Country.getCountryName("EE")

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.previousRegistrations.routes.SchemeStillActiveController.onPageLoad(waypoints, "EE", index, index).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeStillActiveView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country, index, index)(request, messages(application)).toString
        }
      }

    }

  }
}
