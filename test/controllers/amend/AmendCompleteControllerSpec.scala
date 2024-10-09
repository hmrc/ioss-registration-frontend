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

package controllers.amend

import base.SpecBase
import config.Constants.maxSchemes
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.amend.{routes => amendRoutes}
import controllers.routes
import models.amend.RegistrationWrapper
import models.domain.PreviousSchemeDetails
import models.euDetails.EuOptionalDetails
import models.external.ExternalEntryUrl
import models.previousRegistrations.PreviousRegistrationDetails
import models.{Country, TradingName, UserAnswers, Website}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessContactDetailsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.AllEuOptionalDetailsQuery
import queries.previousRegistration.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNames
import queries.{AllWebsites, OriginalRegistrationQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.amend.AmendCompleteView

class AmendCompleteControllerSpec extends SpecBase with MockitoSugar {

  private val mockRegistrationConnector = mock[RegistrationConnector]

  private val userAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      BusinessContactDetailsPage.toString -> Json.obj(
        "fullName" -> "value 1",
        "telephoneNumber" -> "value 2",
        "emailAddress" -> "test@test.com",
        "websiteAddress" -> "value 4",
      )
    ),
    vatInfo = Some(vatCustomerInfo)
  )

  private val originalRegistration = userAnswers.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.registration).success.value


  "AmendComplete Controller" - {

    "when the scheme has started" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(originalRegistration))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

        running(application) {
          val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]
          implicit val msgs: Messages = messages(application)
          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(userAnswers, Some(registrationWrapper)))

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            vrn,
            config.feedbackUrl(request),
            None,
            yourAccountUrl,
            "Company name",
            summaryList
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery and the correct view for a GET with no user answers" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
          val result = route(application, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when there are amended answers" - {

        "must show row for when trading name added" in {

          val newTradingName = TradingName("NewTradingName")
          val updatedAnswers = originalRegistration
            .set(AllTradingNames, List(newTradingName)).success.value

          val application = applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

          running(application) {
            val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val view = application.injector.instanceOf[AmendCompleteView]
            implicit val msgs: Messages = messages(application)
            val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))
            contentAsString(result) mustBe view(
              vrn,
              config.feedbackUrl(request),
              None,
              yourAccountUrl,
              "Company name",
              summaryList
            )(request, messages(application)).toString

            val expectedRow = TradingNameSummary.amendedAnswersRow(updatedAnswers).get

            summaryList.rows must contain(expectedRow)
          }
        }

        "must show row for when previous Registration added" in {

          val previousRegistration: PreviousRegistrationDetails = PreviousRegistrationDetails(
            previousEuCountry = arbitraryCountry.arbitrary.sample.value,
            previousSchemesDetails = Gen.listOfN(maxSchemes, PreviousSchemeDetails(
              previousScheme = arbitraryPreviousScheme.arbitrary.sample.value,
              previousSchemeNumbers = arbitraryPreviousIossSchemeDetails.arbitrary.sample.value,
              nonCompliantDetails = Some(arbitraryNonCompliantDetails.arbitrary.sample.value)
            )).sample.value
          )
          val updatedAnswers = originalRegistration
            .set(AllPreviousRegistrationsQuery, List(previousRegistration)).success.value

          val application = applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

          running(application) {
            val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val view = application.injector.instanceOf[AmendCompleteView]
            implicit val msgs: Messages = messages(application)
            val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))
            contentAsString(result) mustBe view(
              vrn,
              config.feedbackUrl(request),
              None,
              yourAccountUrl,
              "Company name",
              summaryList
            )(request, messages(application)).toString

            val expectedRow = PreviousRegistrationSummary.amendedAnswersRow(updatedAnswers).get

            summaryList.rows must contain(expectedRow)
          }
        }

        "must show row for when Eu Registration added" in {

          val newEuRegistration = EuOptionalDetails(
            euCountry = arbitraryCountry.arbitrary.sample.value,
            hasFixedEstablishment = Some(true),
            registrationType = Some(arbitraryRegistrationType.arbitrary.sample.value),
            euVatNumber = Some(arbitraryEuVatNumber.sample.value),
            euTaxReference = Some(arbitraryEuTaxReference.sample.value),
            fixedEstablishmentTradingName = Some(arbitraryFixedEstablishmentTradingNamePage.arbitrary.sample.value.toString),
            fixedEstablishmentAddress = Some(arbitraryInternationalAddress.arbitrary.sample.value)
          )
          val updatedAnswers = originalRegistration
            .set(AllEuOptionalDetailsQuery, List(newEuRegistration)).success.value

          val application = applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

          running(application) {
            val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val view = application.injector.instanceOf[AmendCompleteView]
            implicit val msgs: Messages = messages(application)
            val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))
            contentAsString(result) mustBe view(
              vrn,
              config.feedbackUrl(request),
              None,
              yourAccountUrl,
              "Company name",
              summaryList
            )(request, messages(application)).toString

            val expectedRow = EuDetailsSummary.amendedAnswersRow(updatedAnswers).get

            summaryList.rows must contain(expectedRow)
          }
        }

        "must show row for when website added" in {

          val newWebsite = Website("https://www.NewWebsite.co.uk")
          val updatedAnswers = originalRegistration
            .set(AllWebsites, List(newWebsite)).success.value

          val application = applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
          when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

          running(application) {
            val request = FakeRequest(GET, amendRoutes.AmendCompleteController.onPageLoad().url)
            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val view = application.injector.instanceOf[AmendCompleteView]
            implicit val msgs: Messages = messages(application)
            val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))
            contentAsString(result) mustBe view(
              vrn,
              config.feedbackUrl(request),
              None,
              yourAccountUrl,
              "Company name",
              summaryList
            )(request, messages(application)).toString

            val expectedRow = WebsiteSummary.amendedAnswersRow(updatedAnswers).get

            summaryList.rows must contain(expectedRow)
          }
        }
      }
    }
  }

  private def getAmendedRegistrationSummaryList(
                                                 answers: UserAnswers,
                                                 registrationWrapper: Option[RegistrationWrapper]
                                               )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.amendedRow(answers)
    val tradingNameSummaryRow = TradingNameSummary.amendedAnswersRow(answers)
    val removedTradingNameRow = TradingNameSummary.removedAnswersRow(getRemovedTradingNames(answers, registrationWrapper))
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.amendedRow(answers)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.amendedAnswersRow(answers)
    val taxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.amendedRow(answers)
    val euDetailsSummaryRow = EuDetailsSummary.amendedAnswersRow(answers)
    val removedEuDetailsRow = EuDetailsSummary.removedAnswersRow(getRemovedEuDetailsRow(answers, registrationWrapper))
    val websiteSummaryRow = WebsiteSummary.amendedAnswersRow(answers)
    val removedWebsiteRow = WebsiteSummary.removedAnswersRow(getRemovedWebsites(answers, registrationWrapper))
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.amendedRowContactName(answers)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.amendedRowTelephoneNumber(answers)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.amendedRowEmailAddress(answers)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.amendedRowAccountName(answers)
    val bankDetailsBicSummaryRow = BankDetailsSummary.amendedRowBIC(answers)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.amendedRowIBAN(answers)

    Seq(
      hasTradingNameSummaryRow,
      tradingNameSummaryRow,
      removedTradingNameRow,
      previouslyRegisteredSummaryRow,
      previousRegistrationSummaryRow,
      taxRegisteredInEuSummaryRow,
      euDetailsSummaryRow,
      removedEuDetailsRow,
      websiteSummaryRow,
      removedWebsiteRow,
      businessContactDetailsContactNameSummaryRow,
      businessContactDetailsTelephoneSummaryRow,
      businessContactDetailsEmailSummaryRow,
      bankDetailsAccountNameSummaryRow,
      bankDetailsBicSummaryRow,
      bankDetailsIbanSummaryRow
    ).flatten
  }

  private def getRemovedTradingNames(answers: UserAnswers, registrationWrapper: Option[RegistrationWrapper]): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNames).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.map(_.registration.tradingNames.map(_.tradingName)).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)

  }

  private def getRemovedEuDetailsRow(answers: UserAnswers, registrationWrapper: Option[RegistrationWrapper]): Seq[Country] = {

    val amendedAnswers = answers.get(AllEuOptionalDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.map(_.registration.schemeDetails.euRegistrationDetails.map(_.issuedBy)).getOrElse(List.empty)

    val removedCountryCodes = originalAnswers.diff(amendedAnswers)

    removedCountryCodes.flatMap(Country.fromCountryCode)
  }

  private def getRemovedWebsites(answers: UserAnswers, registrationWrapper: Option[RegistrationWrapper]): Seq[String] = {

    val amendedAnswers = answers.get(AllWebsites).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.map(_.registration.schemeDetails.websites.map(_.websiteAddress)).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)
  }
}
