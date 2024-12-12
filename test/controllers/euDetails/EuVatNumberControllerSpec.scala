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
import forms.euDetails.EuVatNumberFormProvider
import models.core.{Match, MatchType}
import models.euDetails.RegistrationType
import models.{CheckMode, Country, CountryWithValidationDetails, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.euDetails.*
import pages.rejoin.RejoinRegistrationPage
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuVatNumberView

import scala.concurrent.Future

class EuVatNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val emptyWaypoints: Waypoints = EmptyWaypoints
  private val countryIndex: Index = Index(0)
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == country.code).value

  val formProvider = new EuVatNumberFormProvider()
  val form: Form[String] = formProvider(country)

  private val answers: UserAnswers = basicUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), RegistrationType.VatNumber).success.value

  private lazy val euVatNumberRoute: String = routes.EuVatNumberController.onPageLoad(emptyWaypoints, countryIndex).url

  private def euVatNumberSubmitRoute(waypoints: Waypoints): String = routes.EuVatNumberController.onSubmit(waypoints, countryIndex).url

  private lazy val amendEuVatNumberSubmitRoute: String = routes.EuVatNumberController.onSubmit(amendWaypoints, countryIndex).url

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val rejoinWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, RejoinRegistrationPage.urlFragment))

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    "33333333",
    None,
    "DE",
    None,
    None,
    None,
    None,
    None
  )

  private val nonAmendWaypointsOptions = Table(
    "Not amend waypoint",
    EmptyWaypoints,
    rejoinWaypoints
  )


  private val allWaypointsOptions = Table(
    "Not amend waypoint",
    EmptyWaypoints,
    rejoinWaypoints,
    amendWaypoints
  )

  "EuVatNumber Controller" - {

    "GET" - {
      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, euVatNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EuVatNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, emptyWaypoints, countryIndex, countryWithValidation)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = answers.set(EuVatNumberPage(countryIndex), "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, euVatNumberRoute)

          val view = application.injector.instanceOf[EuVatNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), emptyWaypoints, countryIndex, countryWithValidation)(request, messages(application)).toString
        }
      }
    }

    "must save and redirect to the next page when valid data is submitted when not in amend" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>
        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector)
            )
            .build()

        running(application) {

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn
            Future.successful(None)

          val request =
            FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
              .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(nonAmendsWaypoints, answers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when not in Amend matchType=FixedEstablishmentActiveNETP" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector)
            ).build()

        running(application) {

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(genericMatch))

          val request =
            FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
              .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(nonAmendsWaypoints, country.code).url
        }
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when not in Amend and matchType=TraderIdActiveNETP" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector)
            ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdActiveNETP)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request =
            FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
              .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(nonAmendsWaypoints, country.code).url
        }
      }
    }

    "must redirect to FixedEstablishmentVRNAlreadyRegisteredController page when not in Amend and matchType=OtherMSNETPActiveNETP" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector)
            ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPActiveNETP)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request =
            FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
              .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.euDetails.routes.FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(nonAmendsWaypoints, country.code).url
        }
      }
    }

    "must redirect to ExcludedVRNController page when not in Amend and the vat number is excluded for match FixedEstablishmentQuarantinedNETP " in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request = FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
        }
      }
    }

    "must redirect to ExcludedVRNController page when not in Amend and the vat number is excluded for match TraderIdQuarantinedNETP " in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request = FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
        }
      }
    }

    "must redirect to ExcludedVRNController page when not in Amend and the vat number is excluded for match OtherMSNETPQuarantinedNETP " in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request = FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.euDetails.routes.ExcludedVRNController.onPageLoad().url
        }
      }
    }

    "must redirect to the next page when there is no active trader when not in amend" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))

          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request = FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(nonAmendsWaypoints, answers, expectedAnswers).url
        }
      }
    }

    "must redirect to the next page when there is no excluded trader when not in amend" in {
      forAll(nonAmendWaypointsOptions) { nonAmendsWaypoints =>
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          val expectedResponse = genericMatch.copy(matchType = MatchType.TransferringMSID)

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(Option(expectedResponse))

          val request = FakeRequest(POST, euVatNumberSubmitRoute(nonAmendsWaypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(nonAmendsWaypoints, answers, expectedAnswers).url
        }
      }
    }

    "must redirect to the next page when no active match found" in {
      forAll(allWaypointsOptions) { waypoints =>

        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          ).build()

        running(application) {

          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
            Future.successful(None)

          val request = FakeRequest(POST, euVatNumberSubmitRoute(waypoints))
            .withFormUrlEncodedBody(("value", euVatNumber))

          val result = route(application, request).value

          val expectedAnswers = answers.set(EuVatNumberPage(countryIndex), euVatNumber).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual EuVatNumberPage(countryIndex).navigate(waypoints, answers, expectedAnswers).url
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      forAll(allWaypointsOptions) { waypointOption =>
        val mockRegistrationConnector = mock[RegistrationConnector]

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, euVatNumberSubmitRoute(waypointOption))
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[EuVatNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypointOption, countryIndex, countryWithValidation)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(emptyWaypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(emptyWaypoints).url
      }
    }
  }

  "inAmend" - {

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match FixedEstablishmentQuarantinedNETP when in Amend" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {
        val expectedResponse = genericMatch.copy(matchType = MatchType.FixedEstablishmentQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match TraderIdQuarantinedNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {

        val expectedResponse = genericMatch.copy(matchType = MatchType.TraderIdQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }

    "must not redirect to ExcludedVRNController page when the vat number is excluded for match OtherMSNETPQuarantinedNETP" in {
      val mockRegistrationConnector = mock[RegistrationConnector]
      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        ).build()

      running(application) {
        val expectedResponse = genericMatch.copy(matchType = MatchType.OtherMSNETPQuarantinedNETP)

        when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(euVatNumber), eqTo(country.code))(any(), any())) thenReturn
          Future.successful(Option(expectedResponse))

        when(mockRegistrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWrapper)))

        val request = FakeRequest(POST, amendEuVatNumberSubmitRoute)
          .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.euDetails.routes.FixedEstablishmentTradingNameController.onPageLoad(amendWaypoints, countryIndex).url

        verify(mockRegistrationConnector).getRegistration()(any())
      }
    }
  }
}
