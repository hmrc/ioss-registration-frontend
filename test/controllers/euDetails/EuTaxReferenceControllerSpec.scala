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
import forms.euDetails.EuTaxReferenceFormProvider
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, Index}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuTaxReferenceView

class EuTaxReferenceControllerSpec extends SpecBase with MockitoSugar {
  
  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  
  val formProvider = new EuTaxReferenceFormProvider()
  val form: Form[String] = formProvider(country)
  
  private val answers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(SellsGoodsToEuConsumerMethodPage(countryIndex), EuConsumerSalesMethod.FixedEstablishment).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.TaxId).success.value

  lazy val euTaxReferenceRoute: String = routes.EuTaxReferenceController.onPageLoad(waypoints, countryIndex).url

  "EuTaxReference Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuTaxReferenceView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answers.set(EuTaxReferencePage(countryIndex), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val view = application.injector.instanceOf[EuTaxReferenceView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill("answer"), waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must save and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value
        val expectedAnswers = answers.set(EuTaxReferencePage(countryIndex), "answer").success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe EuTaxReferencePage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuTaxReferenceView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
