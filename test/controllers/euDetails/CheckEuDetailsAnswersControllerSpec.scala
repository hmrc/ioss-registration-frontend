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
import models.euDetails.{EuConsumerSalesMethod, RegistrationType}
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails._
import viewmodels.govuk.SummaryListFluency
import views.html.euDetails.CheckEuDetailsAnswersView

class CheckEuDetailsAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(SellsGoodsToEuConsumerMethodPage(countryIndex), EuConsumerSalesMethod.FixedEstablishment).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.TaxId).success.value
    .set(EuTaxReferencePage(countryIndex), "123456789").success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), "Trading name").success.value
    .set(FixedEstablishmentAddressPage(countryIndex), arbitraryInternationalAddress.arbitrary.sample.value).success.value

  "CheckEuDetailsAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckEuDetailsAnswersView]

        val list = SummaryListViewModel(
          Seq(
            RegistrationTypeSummary.row(answers, waypoints, countryIndex, CheckEuDetailsAnswersPage(countryIndex)),
            EuTaxReferenceSummary.row(answers, waypoints, countryIndex, CheckEuDetailsAnswersPage(countryIndex)),
            FixedEstablishmentTradingNameSummary.row(answers, waypoints, countryIndex, CheckEuDetailsAnswersPage(countryIndex)),
            FixedEstablishmentAddressSummary.row(answers, waypoints, countryIndex, CheckEuDetailsAnswersPage(countryIndex))).flatten
        )

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, countryIndex, country, list)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to the next page when answers are complete on a POST" in {

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      running(application) {
        val request = FakeRequest(POST, routes.CheckEuDetailsAnswersController.onSubmit(waypoints, countryIndex, false).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, answers, answers).url
      }
    }
  }
}
