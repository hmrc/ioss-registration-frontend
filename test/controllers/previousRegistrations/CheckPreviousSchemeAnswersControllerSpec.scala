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
import forms.previousRegistrations.CheckPreviousSchemeAnswersFormProvider
import models.domain.PreviousSchemeNumbers
import models.{Country, Index, PreviousScheme}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.previousRegistration.AllPreviousSchemesForCountryWithOptionalVatNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import viewmodels.checkAnswers.previousRegistrations.PreviousSchemeSummary
import viewmodels.govuk.SummaryListFluency
import views.html.previousRegistrations.CheckPreviousSchemeAnswersView

import scala.concurrent.Future

class CheckPreviousSchemeAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach{

  private val index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints
  private val country = Country.euCountries.head
  private val formProvider = new CheckPreviousSchemeAnswersFormProvider()
  private val form = formProvider(country)
  private val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

  private val baseUserAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviouslyRegisteredPage, true).success.value
      .set(PreviousEuCountryPage(index), country).success.value
      .set(PreviousSchemePage(index, index), PreviousScheme.values.head).success.value
      .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("123456789", None)).success.value

  private lazy val checkPreviousSchemeAnswersRoute = controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockSessionRepository)
  }

  "CheckPreviousSchemeAnswers Controller" - {

    "must return OK and the correct view for a GET when answers are complete" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, checkPreviousSchemeAnswersRoute)
        val authDataRequest = AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), baseUserAnswers, None)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckPreviousSchemeAnswersView]

        val previousSchemes = baseUserAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(index)).get

        val lists = PreviousSchemeSummary.getSummaryLists(previousSchemes, index, country, Seq.empty, waypoints)(authDataRequest, msgs)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, lists, index, country, canAddScheme = true)(request, messages(application)).toString
      }
    }


    "must redirect to Journey Recovery if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, checkPreviousSchemeAnswersRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "on a POST" - {

      "must save the answer and redirect to the next page when valid data is submitted" in {
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(baseUserAnswers))
            .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onSubmit(waypoints, index).url)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value
          val expectedAnswers = baseUserAnswers.set(CheckPreviousSchemeAnswersPage(index), true).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual CheckPreviousSchemeAnswersPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, checkPreviousSchemeAnswersRoute)
              .withFormUrlEncodedBody(("value", ""))

          val authDataRequest = AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), Some(iossNumber), baseUserAnswers, None)

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[CheckPreviousSchemeAnswersView]
          implicit val msgs: Messages = messages(application)

          val previousSchemes = baseUserAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(index)).get

          val lists = PreviousSchemeSummary.getSummaryLists(previousSchemes, index, country, Seq.empty, waypoints)(authDataRequest, msgs)

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, lists, index, country, canAddScheme = true)(request, implicitly).toString
        }
      }

      "must redirect to Journey Recovery if user answers are empty" in {

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onSubmit(waypoints, index).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
