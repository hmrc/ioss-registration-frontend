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
import models.euDetails.RegistrationType
import models.{Country, Index}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.EmptyWaypoints
import pages.euDetails._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import views.html.euDetails.CannotAddCountryView

import scala.concurrent.Future

class CannotAddCountryControllerSpec extends SpecBase {

  private val countryIndex: Index = Index(0)
  private val countryIndex1: Index = Index(1)
  private val country = Country.euCountries.head

  private lazy val cannotAddCountryRoute = routes.CannotAddCountryController.onSubmit(EmptyWaypoints, countryIndex).url

  private val answers =
    basicUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfo.copy(partOfVatGroup = true)))
      .set(TaxRegisteredInEuPage, true).success.value
      .set(EuCountryPage(countryIndex), country).success.value

  "CannotAddCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotAddCountryController.onPageLoad(EmptyWaypoints, countryIndex).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotAddCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(EmptyWaypoints, countryIndex)(request, messages(application)).toString
      }
    }

    "must delete a record and redirect to the next page when the user submits" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, cannotAddCountryRoute)

        val result = route(application, request).value
        val expectedAnswers = answers.remove(EuDetailsQuery(countryIndex)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.TaxRegisteredInEuController.onPageLoad(EmptyWaypoints).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must delete a record and redirect to the next page when the user submits and another record exists" in {

      val answers =
        basicUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfo.copy(partOfVatGroup = true)))
          .set(TaxRegisteredInEuPage, true).success.value
          .set(EuCountryPage(countryIndex), country).success.value
          .set(RegistrationTypePage(countryIndex), RegistrationType.VatNumber).success.value
          .set(EuVatNumberPage(countryIndex), "ATU12345678").success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(countryIndex1), country).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, cannotAddCountryRoute)

        val result = route(application, request).value
        val expectedAnswers = answers.remove(EuDetailsQuery(countryIndex)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.AddEuDetailsController.onPageLoad(EmptyWaypoints).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

  }
}
