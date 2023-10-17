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
import forms.euDetails.AddEuDetailsFormProvider
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.EuDetailsCompletionChecks.getAllIncompleteEuDetails
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.EuDetailsSummary
import views.html.euDetails.AddEuDetailsView
import models.requests.AuthenticatedDataRequest
import play.api.mvc.{AnyContent, AnyContentAsEmpty}

class AddEuDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  //private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val country: Country = Country.euCountries.head

  val formProvider = new AddEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider()

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(SellsGoodsToEuConsumerMethodPage(countryIndex), EuConsumerSalesMethod.FixedEstablishment).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.TaxId).success.value
    .set(EuTaxReferencePage(countryIndex), "123456789").success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), "Trading name").success.value
    .set(FixedEstablishmentAddressPage(countryIndex), arbitraryInternationalAddress.arbitrary.sample.value).success.value

  lazy val addEuDetailsRoute: String = routes.AddEuDetailsController.onPageLoad(waypoints).url

  "AddEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val list = EuDetailsSummary.countryAndVatNumberList(answers, waypoints, AddEuDetailsPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, list, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of eu registrations have already been added" in {

      val userAnswers = (0 to Country.euCountries.size).foldLeft(answers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(EuCountryPage(Index(index)), country).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val view = application.injector.instanceOf[AddEuDetailsView]

        val list = EuDetailsSummary.countryAndVatNumberList(userAnswers, waypoints, AddEuDetailsPage())

        val authenticatedDataRequest: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(request, testCredentials, vrn, userAnswers)

        val incomplete = getAllIncompleteEuDetails()(authenticatedDataRequest)

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints, list, canAddEuDetails = false, incomplete)(request, messages(application)).toString
      }
    }

    "must allow adding an eu registration when just below the maximum number of eu registrations" in {
      val userAnswers = (0 until(Country.euCountries.size - 1)).foldLeft(answers) { case (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(EuCountryPage(Index(index)), country).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val view = application.injector.instanceOf[AddEuDetailsView]

        val list = EuDetailsSummary.countryAndVatNumberList(userAnswers, waypoints, AddEuDetailsPage())

        val authenticatedDataRequest: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(request, testCredentials, vrn, userAnswers)

        val incomplete = getAllIncompleteEuDetails()(authenticatedDataRequest)

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, list, canAddEuDetails = true, incomplete)(request, messages(application)).toString
      }
    }

    "must save redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, s"$addEuDetailsRoute?incompletePromptShown=false")
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = answers.set(AddEuDetailsPage(), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe AddEuDetailsPage().navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, s"$addEuDetailsRoute?incompletePromptShown=true")
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddEuDetailsView]

        val result = route(application, request).value

        val list = EuDetailsSummary.countryAndVatNumberList(answers, waypoints, AddEuDetailsPage())

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, list, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$addEuDetailsRoute?incompletePromptShown=false")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
