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

package controllers.euDetails

import base.SpecBase
import models.{Country, Index}
import pages.EmptyWaypoints
import pages.euDetails.EuCountryPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.euDetails.FixedEstablishmentVRNAlreadyRegisteredView

class FixedEstablishmentVRNAlreadyRegisteredControllerSpec extends SpecBase {

  private val countryIndex: Index = Index(0)

  "FixedEstablishmentVRNAlreadyRegistered Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val country: Country = arbitraryCountry.arbitrary.sample.value

        val userAnswers = basicUserAnswersWithVatInfo.set(EuCountryPage(countryIndex), country).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(EmptyWaypoints, countryIndex).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FixedEstablishmentVRNAlreadyRegisteredView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country.name)(request, messages(application)).toString
        }
      }

    }

  }
}
