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
import forms.previousRegistrations.DeletePreviousSchemeFormProvider
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{Country, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.{DeletePreviousSchemePage, PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.PreviousSchemeForCountryQuery
import repositories.AuthenticatedUserAnswersRepository
import viewmodels.checkAnswers.previousRegistrations.{DeletePreviousSchemeSummary, PreviousSchemeNumberSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.previousRegistrations.DeletePreviousSchemeView

import scala.concurrent.Future

class DeletePreviousSchemeControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints
  private val country: Country = arbitrary[Country].sample.value
  private val formProvider = new DeletePreviousSchemeFormProvider()
  private val form = formProvider(country)
  private val index = Index(0)
  private val previousSchemeNumbers = PreviousSchemeNumbers("012345678", None)
  private val previousScheme = PreviousSchemeDetails(PreviousScheme.OSSU, previousSchemeNumbers, None)
  private val previousRegistration = PreviousRegistrationDetails(country, Seq(previousScheme))

  private val baseUserAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviousEuCountryPage(index), previousRegistration.previousEuCountry).success.value
      .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
      .set(PreviousOssNumberPage(index, index), previousSchemeNumbers).success.value

  private lazy val deletePreviousSchemeRoute = controllers.previousRegistrations.routes.DeletePreviousSchemeController.onPageLoad(waypoints, index, index).url

  "DeletePreviousScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      implicit val msgs: Messages = messages(application)
      val list = SummaryListViewModel(
        Seq(
          DeletePreviousSchemeSummary.row(baseUserAnswers, index, index),
          PreviousSchemeNumberSummary.row(baseUserAnswers, index, index)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, deletePreviousSchemeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeletePreviousSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, index,
          country, list, isLastPreviousScheme = true)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseUserAnswers.copy().set(DeletePreviousSchemePage(index, index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      implicit val msgs: Messages = messages(application)
      val list = SummaryListViewModel(
        Seq(
          DeletePreviousSchemeSummary.row(baseUserAnswers, index, index),
          PreviousSchemeNumberSummary.row(baseUserAnswers, index, index)
        ).flatten
      )

      running(application) {
        val request = FakeRequest(GET, deletePreviousSchemeRoute)

        val view = application.injector.instanceOf[DeletePreviousSchemeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), waypoints, index, index, country, list, isLastPreviousScheme = true)(request, messages(application)).toString
      }
    }

    "must delete a scheme and redirect to the next page when user answers Yes" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousSchemeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseUserAnswers.remove(PreviousSchemeForCountryQuery(index, index)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual DeletePreviousSchemePage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not delete a scheme and redirect to the next page when user answers No" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousSchemeRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe
          DeletePreviousSchemePage(index, index).navigate(waypoints, baseUserAnswers, baseUserAnswers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      implicit val msgs: Messages = messages(application)
      val list = SummaryListViewModel(
        Seq(
          DeletePreviousSchemeSummary.row(baseUserAnswers, index, index),
          PreviousSchemeNumberSummary.row(baseUserAnswers, index, index)
        ).flatten
      )

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousSchemeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeletePreviousSchemeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index, index, country, list, isLastPreviousScheme = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deletePreviousSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousSchemeRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
