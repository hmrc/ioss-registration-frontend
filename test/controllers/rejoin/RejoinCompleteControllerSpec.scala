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

package controllers.rejoin

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.UserAnswers
import models.amend.RegistrationWrapper
import models.domain.VatCustomerInfo
import models.external.ExternalEntryUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessContactDetailsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.OriginalRegistrationQuery
import queries.rejoin.NewIossReferenceQuery
import queries.tradingNames.AllTradingNames
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinCompleteView

import java.time.LocalDate
import scala.concurrent.Future

class RejoinCompleteControllerSpec extends SpecBase with MockitoSugar {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]
  private val commencementDate = LocalDate.now(stubClockAtArbitraryDate)
  private val returnStartDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
  private val includedSalesDate = commencementDate.withDayOfMonth(1)

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
  ).set(NewIossReferenceQuery, "IM900100000002").success.value

  private val originalRegistration = userAnswers.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.registration).success.value

  "RejoinCompleteController" - {

    "return OK and the correct view when onPageLoad is called and data is available" in {

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Future.successful(None)
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Future.successful(Right(registrationWrapper))

      val application = applicationBuilder(userAnswers = Some(originalRegistration))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinCompleteController.onPageLoad().url)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val view = application.injector.instanceOf[RejoinCompleteView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(userAnswers, Some(registrationWrapper)))

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          vrn,
          config.feedbackUrl(request),
          None,
          yourAccountUrl,
          "Company name",
          "IM900100000002",
          commencementDate,
          returnStartDate,
          includedSalesDate,
          None,
          1,
          summaryList
        )(request, messages(application)).toString
      }
    }

    "return runtimeException when NewIossRefrence not set" in {

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Future.successful(None)
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Future.successful(Right(registrationWrapper))

      val userAnswers = UserAnswers(
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

      val originalRegistration = userAnswers.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.registration).success.value

      val application = applicationBuilder(userAnswers = Some(originalRegistration))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinCompleteController.onPageLoad().url)

        val exception = intercept[RuntimeException] {
          val result = route(application, request).value
          status(result) mustEqual OK
        }

        exception.getMessage mustEqual "NewIossReference has not been set in answers"
      }
    }

    "return runtimeException when Company name is not set" in {

      when(mockRegistrationConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Future.successful(None)
      when(mockRegistrationConnector.getRegistration()(any())) thenReturn Future.successful(Right(registrationWrapper))

      val vatCustomerInfo: VatCustomerInfo =
        VatCustomerInfo(
          registrationDate = LocalDate.now(stubClockAtArbitraryDate),
          desAddress = arbitraryDesAddress.arbitrary.sample.value,
          partOfVatGroup = false,
          organisationName = None,
          individualName = None,
          singleMarketIndicator = true,
          deregistrationDecisionDate = None,
          overseasIndicator = false
        )

      val userAnswers = UserAnswers(
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
      ).set(NewIossReferenceQuery, "IM900100000002").success.value

      val originalRegistration = userAnswers.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.registration).success.value

      val application = applicationBuilder(userAnswers = Some(originalRegistration))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinCompleteController.onPageLoad().url)

        val exception = intercept[RuntimeException] {
          val result = route(application, request).value
          status(result) mustEqual OK
        }

        exception.getMessage mustEqual "OrganisationName has not been set in answers"
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

}
