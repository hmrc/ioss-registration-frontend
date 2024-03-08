/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.RegistrationConnector
import controllers.actions.{FakeIossRequiredAction, IossRequiredAction}
import forms.amend.ViewOrChangePreviousRegistrationFormProvider
import models.UserAnswers
import models.amend.PreviousRegistration
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{ViewOrChangePreviousRegistrationPage, ViewOrChangePreviousRegistrationsMultiplePage}
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.PreviousRegistrationIossNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import services.AccountService
import utils.FutureSyntax.FutureOps
import views.html.amend.ViewOrChangePreviousRegistrationView

class ViewOrChangePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val previousRegistrations: Seq[PreviousRegistration] = Gen.listOfN(4, arbitraryPreviousRegistration.arbitrary).sample.value

  private val previousRegistration: PreviousRegistration = previousRegistrations.head
  override val iossNumber: String = previousRegistration.iossNumber

  private val formProvider = new ViewOrChangePreviousRegistrationFormProvider()
  private val form: Form[Boolean] = formProvider(iossNumber)

  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val viewOrChangePreviousRegistrationRoute: String = routes.ViewOrChangePreviousRegistrationController.onPageLoad(waypoints).url

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockAccountService)
  }

  "ViewOrChangePreviousRegistration Controller" - {

    "must return OK and the correct view for a GET when a single previous registration exists" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(emptyUserAnswersWithVatInfo), registrationWrapper)))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, iossNumber)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture

      val userAnswers = emptyUserAnswersWithVatInfo.set(ViewOrChangePreviousRegistrationPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(userAnswers), registrationWrapper)))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints, iossNumber)(request, messages(application)).toString
      }
    }

    "must redirect to the next page on a GET when multiple previous registrations exist" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(emptyUserAnswersWithVatInfo), registrationWrapper)))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationsMultiplePage.route(waypoints).url
      }
    }

    "must throw an Illegal State Exception on a GET when no previous registrations exist" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq.empty.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(emptyUserAnswersWithVatInfo), registrationWrapper)))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

        val result = route(application, request).value.failed

        whenReady(result) { exp =>
          exp mustBe a[IllegalStateException]
          exp.getMessage must include("Must have one or more previous registrations")
        }
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
          .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(emptyUserAnswersWithVatInfo), registrationWrapper)))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(ViewOrChangePreviousRegistrationPage, true).success.value
          .set(PreviousRegistrationIossNumberQuery, iossNumber).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationPage.navigate(waypoints, emptyUserAnswersWithVatInfo, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(emptyUserAnswersWithVatInfo), registrationWrapper)))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, iossNumber)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
