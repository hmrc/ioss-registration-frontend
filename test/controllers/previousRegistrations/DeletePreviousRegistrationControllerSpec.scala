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
import controllers.routes
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{Country, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{DeletePreviousRegistrationPage, PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.PreviousRegistrationQuery
import repositories.{AuthenticatedUserAnswersRepository}
import views.html.previousRegistrations.DeletePreviousRegistrationView

import scala.concurrent.Future

class DeletePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val formProvider = new DeletePreviousRegistrationFormProvider()
  private val form = formProvider()

  private val index = Index(0)
  private val country = Country.euCountries.head
  private val previousSchemeNumbers = PreviousSchemeNumbers("VAT Number", None)
  private val previousScheme = PreviousSchemeDetails(PreviousScheme.OSSU, previousSchemeNumbers)
  private val previousRegistration = PreviousRegistrationDetails(country, List(previousScheme))

  private lazy val deletePreviousRegistrationRoute = controllers.previousRegistrations.routes.DeletePreviousRegistrationController.onPageLoad(
    waypoints, index).url

  private val baseUserAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviousEuCountryPage(index), previousRegistration.previousEuCountry).success.value
      .set(PreviousOssNumberPage(index, index), previousSchemeNumbers).success.value

  "DeletePreviousRegistration Controller" - {

//    "must return OK and the correct view for a GET" in {
//
//      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()
//
//      running(application) {
//        val request = FakeRequest(GET, deletePreviousRegistrationRoute)
//
//        val result = route(application, request).value
//
//        val view = application.injector.instanceOf[DeletePreviousRegistrationView]
//
//        status(result) mustEqual OK
//        contentAsString(result) mustEqual view(form, waypoints, index, previousRegistration.previousEuCountry.name)(request, messages(application)).toString
//
//      }
//    }

//    "must delete a record and redirect to the next page when the user answers Yes" in {
//
//      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
//
//      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
//
//      val application =
//        applicationBuilder(userAnswers = Some(baseUserAnswers))
//          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
//          .build()
//
//      running(application) {
//        val request =
//          FakeRequest(POST, deletePreviousRegistrationRoute)
//            .withFormUrlEncodedBody(("value", "true"))
//
//        val result = route(application, request).value
//        val expectedAnswers =
//          baseUserAnswers
//            .remove(PreviousRegistrationQuery(index)).success.value
//
//        status(result) mustEqual SEE_OTHER
//        redirectLocation(result).value mustEqual DeletePreviousRegistrationPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
//        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
//      }
//
//    }
//
//    "must not delete a record and redirect to the next page when the user answers No" in {
//
//      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
//
//      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
//
//      val application =
//        applicationBuilder(userAnswers = Some(baseUserAnswers))
//          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
//          .build()
//
//      running(application) {
//        val request =
//          FakeRequest(POST, deletePreviousRegistrationRoute)
//            .withFormUrlEncodedBody(("value", "false"))
//
//        val result = route(application, request).value
//
//        status(result) mustEqual SEE_OTHER
//        redirectLocation(result).value mustEqual DeletePreviousRegistrationPage(index).navigate(waypoints, emptyUserAnswers, baseUserAnswers).url
//        verify(mockSessionRepository, never()).set(any())
//      }
//    }
//
//    "must return a Bad Request and errors when invalid data is submitted" in {
//
//      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()
//
//      running(application) {
//        val request =
//          FakeRequest(POST, deletePreviousRegistrationRoute)
//            .withFormUrlEncodedBody(("value", ""))
//
//        val boundForm = form.bind(Map("value" -> ""))
//
//        val view = application.injector.instanceOf[DeletePreviousRegistrationView]
//
//        val result = route(application, request).value
//
//        status(result) mustEqual BAD_REQUEST
//        contentAsString(result) mustEqual view(
//          boundForm, waypoints, index, previousRegistration.previousEuCountry.name)(request, messages(application)).toString
//      }
//    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deletePreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no EU VAT details exist" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, deletePreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
