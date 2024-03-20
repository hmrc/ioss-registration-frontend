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
import connectors.RegistrationConnector
import controllers.amend.{routes => amendRoutes}
import models.amend.{PreviousRegistration, RegistrationWrapper}
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.etmp.amend.AmendRegistrationResponse
import models.responses.InternalServerError
import models.{CheckMode, Index, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.euDetails.{EuCountryPage, TaxRegisteredInEuPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import queries.PreviousRegistrationIossNumberQuery
import queries.euDetails.EuDetailsQuery
import services._
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.SummaryListFluency
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.amend.ChangeRegistrationView

import java.time.{Clock, LocalDate, LocalDateTime}
import scala.concurrent.Future

class ChangeRegistrationControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val previousRegistrationWaypoints: Waypoints =
    EmptyWaypoints.setNextWaypoint(Waypoint(ChangePreviousRegistrationPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))
  private val amendYourAnswersPage = ChangeRegistrationPage
  private val amendYourPreviousAnswersPage = ChangePreviousRegistrationPage
  private val country = arbitraryCountry.arbitrary.sample.value
  private val previousRegistration: PreviousRegistration = arbitraryPreviousRegistration.arbitrary.sample.value

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockRegistrationService = mock[RegistrationService]
  private val mockAccountService = mock[AccountService]

  private val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      iossReference = "IM900100000001",
      businessPartner = "businessPartner"
    )

  private val rejoinableRegistration = {
    val registration = registrationWrapper.registration

    registrationWrapper.copy(
      registration = registration.copy(
        exclusions = List(
          EtmpExclusion(
            exclusionReason = NoLongerSupplies,
            effectiveDate = LocalDate.now(),
            decisionDate = LocalDate.now(),
            quarantine = false
          )
        )
      )
    )
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockRegistrationService)
    Mockito.reset(mockAccountService)
  }

  "ChangeRegistration Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET when no previous registrations exist" in {

        val isCurrentIossAccount: Boolean = true

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(rejoinableRegistration).toFuture
        when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq.empty.toFuture

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {

          val request = FakeRequest(GET, amendRoutes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)

          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[ChangeRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))
          val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo, isCurrentIossAccount))

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, vatInfoList, list, iossNumber, isValid = true, hasPreviousRegistrations = false, isCurrentIossAccount = true)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET when previous registrations exist" in {

        val isCurrentIossAccount: Boolean = true

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(rejoinableRegistration).toFuture
        when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {

          val request = FakeRequest(GET, amendRoutes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)

          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[ChangeRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))
          val list = SummaryListViewModel(rows =
            getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo, isCurrentIossAccount, previousRegistrationWaypoints, amendYourPreviousAnswersPage)
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(previousRegistrationWaypoints, vatInfoList, list, iossNumber, isValid = true, hasPreviousRegistrations = true, isCurrentIossAccount = true)(request, messages(application)).toString
          contentAsString(result).contains(msgs("changeRegistration.changePreviousRegistration")) mustBe true
        }
      }

      "must return OK and the correct view for a GET when previous registration currently in focus" in {

        val isCurrentIossAccount: Boolean = false

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(rejoinableRegistration).toFuture
        when(mockAccountService.getPreviousRegistrations()(any())) thenReturn Seq(previousRegistration).toFuture

        val answers = completeUserAnswersWithVatInfo.set(PreviousRegistrationIossNumberQuery, previousRegistration.iossNumber).success.value

        val application = applicationBuilder(userAnswers = Some(answers), clock = Some(Clock.systemUTC()))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AccountService].toInstance(mockAccountService))
          .build()

        running(application) {

          val request = FakeRequest(GET, amendRoutes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)

          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[ChangeRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(answers))
          val list = SummaryListViewModel(rows =
            getChangeRegistrationSummaryList(answers, isCurrentIossAccount, previousRegistrationWaypoints, amendYourPreviousAnswersPage)
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(previousRegistrationWaypoints, vatInfoList, list, previousRegistration.iossNumber, isValid = true, hasPreviousRegistrations = true, isCurrentIossAccount = false)(request, messages(application)).toString
          contentAsString(result).contains(msgs("changeRegistration.toCurrentRegistration")) mustBe true
        }
      }
    }

    ".onSubmit" - {

      "when the user has answered all necessary data and submission of the registration succeeds" - {

        "redirect to the next page" in {

          val registrationConnector = mock[RegistrationConnector]
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
            .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          when(mockRegistrationService.amendRegistration(
            answers = any(),
            registration = any(),
            vrn = any(),
            iossNumber = any(),
            rejoin = ArgumentMatchers.eq(false))(any())) thenReturn Right(amendRegistrationResponse).toFuture

          running(application) {
            val request = FakeRequest(POST, amendRoutes.ChangeRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              ChangeRegistrationPage.navigate(EmptyWaypoints, completeUserAnswersWithVatInfo, completeUserAnswersWithVatInfo).route.url
          }
        }

        "when the user has answered all necessary data but submission of the registration fails" - {

          "redirect to error submitting amendment" in {

            val registrationConnector = mock[RegistrationConnector]
            when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

            val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
              .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
              .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
              .build()

            when(mockRegistrationService.amendRegistration(
              answers = any(),
              registration = any(),
              vrn = any(),
              iossNumber = any(),
              rejoin = ArgumentMatchers.eq(false)
            )(any())) thenReturn Left(InternalServerError).toFuture

            running(application) {
              val request = FakeRequest(POST, amendRoutes.ChangeRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe routes.ErrorSubmittingAmendmentController.onPageLoad().url
            }
          }
        }
      }

      "when the user has not answered all necessary data" - {

        "the page is refreshed when the incomplete prompt was not shown" in {

          val registrationConnector = mock[RegistrationConnector]
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, amendRoutes.ChangeRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url
          }
        }

        "the user is redirected when the incomplete prompt is shown" - {

          "to Has Trading Name when trading names are not populated correctly" in {

            val registrationConnector = mock[RegistrationConnector]
            when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
              .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, amendRoutes.ChangeRegistrationController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustEqual controllers.tradingNames.routes.HasTradingNameController.onPageLoad(waypoints).url
            }
          }

          "to Tax Registered In EU when it has a 'yes' answer but all countries were removed" in {

            val registrationConnector = mock[RegistrationConnector]
            val registrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)
            when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapper)))

            val answers = completeUserAnswers
              .set(TaxRegisteredInEuPage, true).success.value
              .set(EuCountryPage(Index(0)), country).success.value
              .remove(EuDetailsQuery(Index(0))).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, amendRoutes.ChangeRegistrationController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustEqual controllers.euDetails.routes.TaxRegisteredInEuController.onPageLoad(waypoints).url
            }
          }
        }
      }
    }
  }

  private def getChangeRegistrationVatRegistrationDetailsSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {
    Seq(
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowPartOfVatUkGroup(answers),
      VatRegistrationDetailsSummary.rowUkVatRegistrationDate(answers),
      VatRegistrationDetailsSummary.rowBusinessAddress(answers)
    ).flatten
  }

  private def getChangeRegistrationSummaryList(
                                                answers: UserAnswers,
                                                isCurrentIossAccount: Boolean,
                                                waypoints: Waypoints = waypoints,
                                                page: CheckAnswersPage = amendYourAnswersPage
                                              )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.row(answers, waypoints, page, isCurrentIossAccount)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(answers, waypoints, page, isCurrentIossAccount)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, page, lockEditing = false, isCurrentIossAccount)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, Seq.empty, waypoints, page, isCurrentIossAccount)
    val taxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.row(answers, waypoints, page, isCurrentIossAccount)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(answers, waypoints, page, isCurrentIossAccount)
    val websiteSummaryRow = WebsiteSummary.checkAnswersRow(answers, waypoints, page, isCurrentIossAccount)
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(answers, waypoints, page)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(answers, waypoints, page)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(answers, waypoints, page)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(answers, waypoints, page)
    val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(answers, waypoints, page)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(answers, waypoints, page)

    Seq(
      hasTradingNameSummaryRow.map { sr =>
        if (tradingNameSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      tradingNameSummaryRow,
      previouslyRegisteredSummaryRow.map { sr =>
        if (previousRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousRegistrationSummaryRow,
      taxRegisteredInEuSummaryRow.map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow,
      websiteSummaryRow,
      businessContactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      businessContactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      businessContactDetailsEmailSummaryRow,
      bankDetailsAccountNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsBicSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsIbanSummaryRow
    ).flatten
  }
}
