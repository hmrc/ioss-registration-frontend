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
import controllers.previousRegistrations.{routes => prevRoutes}
import models.{Country, Index, NormalMode}
import pages.{EmptyWaypoints, Waypoints}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.previousRegistrations.SchemeQuarantinedView

class SchemeQuarantinedControllerSpec extends SpecBase {

  private val country: Country = Country.euCountries.head

  private val index: Index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints

  "SchemeQuarantinedController Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET in NormalMode" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, prevRoutes.SchemeQuarantinedController.onPageLoad(waypoints, index, index).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeQuarantinedView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(index, index)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET in AmendMode" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, prevRoutes.SchemeQuarantinedController.onPageLoad(waypoints, index, index).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeQuarantinedView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(index, index)(request, messages(application)).toString
        }
      }

    }

  }
}
