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
import formats.Format.{dateFormatter, dateMonthYearFormatter}
import models.{TradingName, UserAnswers}
import models.ossRegistration.OssRegistration
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.etmp.EtmpEnrolmentResponseQuery
import queries.tradingNames.AllTradingNames
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ApplicationCompleteView

import java.time.LocalDate

class ApplicationCompleteControllerSpec extends SpecBase {

  private val commencementDate = LocalDate.now(stubClockAtArbitraryDate)
  private val returnStartDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
  private val includedSalesDate = commencementDate.withDayOfMonth(1)

  private val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

  private val userAnswers: UserAnswers = completeUserAnswersWithVatInfo
    .set(EtmpEnrolmentResponseQuery, etmpEnrolmentResponse).success.value

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("urls.userResearch1" -> "https://test-url.com")
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]
        implicit val msgs: Messages = messages(application)
        val expectedList: SummaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(userAnswers, ossRegistration))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          etmpEnrolmentResponse.iossReference,
          vatCustomerInfo.organisationName.get,
          includedSalesDate.format(dateMonthYearFormatter),
          returnStartDate.format(dateFormatter),
          includedSalesDate.format(dateFormatter),
          config.feedbackUrl(request),
          None,
          expectedList,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when an ossRegistration and answers have been amended" in {

      val newTradingName = TradingName("NewTradingName")
      val updatedAnswers = userAnswers
        .set(AllTradingNames, List(newTradingName)).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers), ossRegistration = ossRegistration)
        .configure("urls.userResearch1" -> "https://test-url.com")
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]
        implicit val msgs: Messages = messages(application)
        val expectedList: SummaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, ossRegistration))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          etmpEnrolmentResponse.iossReference,
          vatCustomerInfo.organisationName.get,
          includedSalesDate.format(dateMonthYearFormatter),
          returnStartDate.format(dateFormatter),
          includedSalesDate.format(dateFormatter),
          config.feedbackUrl(request),
          ossRegistration,
          expectedList,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }
  }

  private def getAmendedRegistrationSummaryList(
                                                 answers: UserAnswers,
                                                 ossRegistration: Option[OssRegistration]
                                               )(implicit msgs: Messages): Seq[SummaryListRow] = {
    val hasTradingNameSummaryRow = HasTradingNameSummary.amendedRow(answers)
    val tradingNameSummaryRow = TradingNameSummary.amendedAnswersRow(answers)
    val removedTradingNameRow = TradingNameSummary.removedAnswersRow(getRemovedTradingNames(answers, ossRegistration))
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

  private def getRemovedTradingNames(answers: UserAnswers, ossRegistration: Option[OssRegistration]): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNames).getOrElse(List.empty)
    val originalAnswers = ossRegistration.map(_.tradingNames).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)

  }
}
