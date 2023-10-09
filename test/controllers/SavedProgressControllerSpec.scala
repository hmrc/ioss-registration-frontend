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
import config.FrontendAppConfig
import connectors.{RegistrationConnector, SaveForLaterConnector, SavedUserAnswers}
import models.external.ExternalEntryUrl
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.SavedProgressPage
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import views.html.SavedProgressView

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId}
import scala.concurrent.Future

class SavedProgressControllerSpec extends SpecBase {

  "SavedProgress Controller" - {

    "must return OK and the correct view for a GET and clear user-answers after registration submitted" in {

      val instantDate = Instant.now
      val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
      val date = LocalDate.now(stubClock).plusDays(28)

      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockUARepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      val savedAnswers = SavedUserAnswers(
        vrn,
        JsObject(Seq("test" -> Json.toJson("test"))),
        None,
        instantDate
      )

      when(mockAppConfig.cacheTtl) thenReturn 1
      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Right(Some(savedAnswers)))
      when(mockUARepository.clear(any())) thenReturn Future.successful(true)
      when(mockUARepository.set(any())) thenReturn Future.successful(true)
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Future.successful(Right(true))
      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      val app = applicationBuilder(userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[AuthenticatedUserAnswersRepository].toInstance(mockUARepository),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad("test").url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[SavedProgressView]

        status(result) mustEqual OK
        verify(mockUARepository, times(1)).set(eqTo(completeUserAnswers.set(SavedProgressPage, "test").success.value.copy(lastUpdated = instantDate)))
        contentAsString(result) mustEqual view(date.format(dateTimeFormatter), "test")(request, messages(app)).toString
      }
    }

    "must return OK and the correct view for a GET and clear user-answers after registration submitted and add the external backToYourAccount url" in {

      val instantDate = Instant.now
      val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
      val date = LocalDate.now(stubClock).plusDays(28)

      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockUARepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      val savedAnswers = SavedUserAnswers(
        vrn,
        JsObject(Seq("test" -> Json.toJson("test"))),
        None,
        instantDate
      )


      when(mockAppConfig.cacheTtl) thenReturn 1
      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Right(Some(savedAnswers)))
      when(mockUARepository.clear(any())) thenReturn Future.successful(true)
      when(mockUARepository.set(any())) thenReturn Future.successful(true)
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Future.successful(Right(true))
      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("/example"))))
      val app = applicationBuilder(userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[AuthenticatedUserAnswersRepository].toInstance(mockUARepository),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad("test").url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[SavedProgressView]


        status(result) mustEqual OK
        verify(mockUARepository, times(1)).set(eqTo(completeUserAnswers.set(SavedProgressPage, "test").success.value.copy(lastUpdated = instantDate)))
        contentAsString(result) mustEqual view( date.format(dateTimeFormatter), "test", Some("/example"))(request, messages(app)).toString
      }
    }

    "must redirect to Journey Recovery when Save For Later Connector returns ConflictFound" in {

      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockUARepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Left(ConflictFound))
      when(mockUARepository.clear(any())) thenReturn Future.successful(false)
      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
              .overrides(
                bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                bind[AuthenticatedUserAnswersRepository].toInstance(mockUARepository),
                bind[RegistrationConnector].toInstance(mockRegistrationConnector)
              ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad("test").url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockUARepository, times(0)).clear(eqTo(completeUserAnswers.id))
      }
    }

    "must redirect to Journey Recovery Controller when Save For Later Connector returns Error Response" in {

      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockUARepository = mock[AuthenticatedUserAnswersRepository]
      val mockRegistrationConnector = mock[RegistrationConnector]

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))
      when(mockUARepository.clear(any())) thenReturn Future.successful(false)
      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[AuthenticatedUserAnswersRepository].toInstance(mockUARepository),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad("test").url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockUARepository, times(0)).clear(eqTo(completeUserAnswers.id))
      }
    }

  }
}
