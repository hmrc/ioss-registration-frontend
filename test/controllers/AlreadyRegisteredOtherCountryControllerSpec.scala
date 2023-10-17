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
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.filters.AlreadyRegisteredOtherCountryView

class AlreadyRegisteredOtherCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "AlreadyRegisteredOtherCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val countryCode: String = "NL"
      val countryName: String = "Netherlands"
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.filters.routes.AlreadyRegisteredOtherCountryController.onPageLoad(countryCode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AlreadyRegisteredOtherCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(countryName)(request, messages(application)).toString
      }
    }
  }
}
