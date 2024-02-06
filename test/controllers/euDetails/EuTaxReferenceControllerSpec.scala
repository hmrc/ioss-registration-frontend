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
import connectors.RegistrationConnector
import forms.euDetails.EuTaxReferenceFormProvider
import models.core.{Match, MatchType}
import models.euDetails.RegistrationType
import models.{CheckMode, Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.euDetails._
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuTaxReferenceView

import scala.concurrent.Future

class EuTaxReferenceControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val euTaxReference = arbitraryEuTaxReference.sample.value

  val formProvider = new EuTaxReferenceFormProvider()
  val form: Form[String] = formProvider(country)

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.TaxId).success.value

  private lazy val euTaxReferenceRoute: String = routes.EuTaxReferenceController.onPageLoad(waypoints, countryIndex).url
  private lazy val euTaxReferenceSubmitRoute: String = routes.EuTaxReferenceController.onSubmit(waypoints, countryIndex).url
  private lazy val amendEuTaxReferenceSubmitRoute: String = routes.EuTaxReferenceController.onSubmit(amendWaypoints, countryIndex).url

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    "333333333",
    None,
    "DE",
    None,
    None,
    None,
    None,
    None
  )

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

      val userAnswers = answers.set(EuTaxReferencePage(countryIndex), euTaxReference).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val view = application.injector.instanceOf[EuTaxReferenceView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(euTaxReference), waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must save and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn
          Future.successful(None)

        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", euTaxReference))

        val result = route(application, request).value
        val expectedAnswers = answers.set(EuTaxReferencePage(countryIndex), euTaxReference).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe EuTaxReferencePage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=FixedEstablishmentActiveNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(genericMatch))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(EmptyWaypoints, country.code).url
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=TraderIdActiveNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(EmptyWaypoints, country.code).url
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when matchType=OtherMSNETPActiveNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(EmptyWaypoints, country.code).url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match FixedEstablishmentQuarantinedNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match TraderIdQuarantinedNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to ExcludedVRNController page when the vat number is excluded for match OtherMSNETPQuarantinedNETP" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
      }
    }

    "must redirect to the next page when there is no active trader" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuTaxReferencePage(countryIndex), taxReferenceNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuTaxReferencePage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must redirect to the next page when there is no excluded trader" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuTaxReferencePage(countryIndex), taxReferenceNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuTaxReferencePage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must redirect to the next page when no active match found" in {

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
        ).build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(None)

        val request = FakeRequest(POST, euTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        val expectedAnswers = answers.set(EuTaxReferencePage(countryIndex), taxReferenceNumber).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuTaxReferencePage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
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

  "inAmend" - {

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match FixedEstablishmentQuarantinedNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]

      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match TraderIdQuarantinedNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match OtherMSNETPQuarantinedNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to FixedEstablishmentVRNAlreadyRegisteredController page when the vat number is excluded for match FixedEstablishmentActiveNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to FixedEstablishmentVRNAlreadyRegisteredController page when the vat number is excluded for match TraderIdActiveNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to FixedEstablishmentVRNAlreadyRegisteredController page when the vat number is excluded for match OtherMSNETPActiveNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val taxReferenceNumber: String = "333333333"

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)

        when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuTaxReferenceSubmitRoute)
          .withFormUrlEncodedBody(("value", taxReferenceNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

  }
}
