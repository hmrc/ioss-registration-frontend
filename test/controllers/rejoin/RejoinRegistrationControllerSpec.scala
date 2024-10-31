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
import config.Constants.correctionsPeriodsLimit
import connectors.{RegistrationConnector, ReturnStatusConnector}
import controllers.rejoin.validation.RejoinRegistrationValidation
import controllers.rejoin.{routes => rejoinRoutes}
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.etmp.amend.AmendRegistrationResponse
import models.responses.InternalServerError
import models.{CheckMode, CurrentReturns, Index, Return, SubmissionStatus, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, IdiomaticMockito}
import pages._
import pages.euDetails.{EuCountryPage, TaxRegisteredInEuPage}
import pages.rejoin.{CannotRejoinRegistrationPage, RejoinRegistrationPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.euDetails.EuDetailsQuery
import services._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.govuk.SummaryListFluency
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import views.html.rejoin.RejoinRegistrationView

import java.time.{Clock, LocalDate, LocalDateTime}
import scala.concurrent.Future

class RejoinRegistrationControllerSpec extends SpecBase with IdiomaticMockito with SummaryListFluency {

  private val rejoinWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinRegistrationPage, CheckMode, RejoinRegistrationPage.urlFragment))
  private val rejoinRegistrationPage = RejoinRegistrationPage
  private val registrationService = mock[RegistrationService]
  private val mockReturnStatusConnector = mock[ReturnStatusConnector]
  private val country = arbitraryCountry.arbitrary.sample.value
  private val registrationValidationFailureRedirect = rejoinRoutes.CannotRejoinController.onPageLoad()

  private val isCurrentIossAccount: Boolean = true

  private val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      iossReference = "IM900100000001",
      businessPartner = "businessPartner"
    )

  "RejoinRegistration Controller" - {

    ".onPageLoad" - {

      "must redirect if registration does not meet requirements" in {

        val registrationConnector = mock[RegistrationConnector]
        val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

        val registrationWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())
        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionOnBoundary)))

        when(rejoinRegistrationValidation.validateEuRegistrations(
          any(),
          any()
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(registrationValidationFailureRedirect)))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
          .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe registrationValidationFailureRedirect.url
        }
      }

      "must return OK and the correct view for a GET when the exclusion is valid" in {

        val registrationConnector = mock[RegistrationConnector]
        val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

        val registrationWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionOnBoundary)))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWithExclusionOnBoundary)
        )
          .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
          .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        when(rejoinRegistrationValidation.validateEuRegistrations(
          ArgumentMatchers.eq(registrationWithExclusionOnBoundary),
          ArgumentMatchers.eq(rejoinWaypoints)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Right(true)))

        running(application) {

          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)

          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[RejoinRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))
          val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

          status(result) mustBe OK
          contentAsString(result) mustBe view(rejoinWaypoints, vatInfoList, list, iossNumber, isValid = true)(request, messages(application)).toString
        }
      }

      "must error when the exclusion is invalid" in {

        val registrationConnector = mock[RegistrationConnector]
        val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

        val registrationWithExclusionInFuture = createRegistrationWrapperWithExclusion(LocalDate.now().plusDays(1))

        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionInFuture)))

        when(rejoinRegistrationValidation.validateEuRegistrations(
          ArgumentMatchers.eq(registrationWithExclusionInFuture),
          ArgumentMatchers.eq(rejoinWaypoints)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Right(true)))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWithExclusionInFuture)
        )
          .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
          .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe CannotRejoinRegistrationPage.route(rejoinWaypoints).url
        }
      }

      "must redirect to Cannot Rejoin Registration Page when there are outstanding returns" in {
        val registrationConnector = mock[RegistrationConnector]
        val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

        val registrationWithExclusionInFuture = createRegistrationWrapperWithExclusion(LocalDate.now())

        val dueReturn = Return(
          firstDay = LocalDate.now(),
          lastDay = LocalDate.now(),
          dueDate = LocalDate.now().minusYears(correctionsPeriodsLimit - 1),
          submissionStatus = SubmissionStatus.Due,
          inProgress = true,
          isOldest = true
        )

        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionInFuture)))

        when(rejoinRegistrationValidation.validateEuRegistrations(
          ArgumentMatchers.eq(registrationWithExclusionInFuture),
          ArgumentMatchers.eq(rejoinWaypoints)
        )(any(), any(), any()))
          .thenReturn(Future.successful(Right(true)))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(dueReturn), finalReturnsCompleted = false)).toFuture

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWithExclusionInFuture)
        )
          .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
          .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe CannotRejoinRegistrationPage.route(rejoinWaypoints).url
        }
      }
    }

    def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate) = {
      val registration = registrationWrapper.registration

      registrationWrapper.copy(
        registration = registration.copy(
          exclusions = List(
            EtmpExclusion(
              exclusionReason = NoLongerSupplies,
              effectiveDate = effectiveDate,
              decisionDate = LocalDate.now(),
              quarantine = false
            )
          )
        )
      )
    }


    ".onSubmit" - {

      "must redirect if registration does not meet requirements" in {

        val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

        val registrationConnector = mock[RegistrationConnector]
        val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

        when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

        when(rejoinRegistrationValidation.validateEuRegistrations(
          any(), any()
        )(any(), any(), any()))
          .thenReturn(Future.successful(Left(registrationValidationFailureRedirect)))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[RegistrationService].toInstance(registrationService))
          .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
          .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = false).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe registrationValidationFailureRedirect.url
        }
      }

      "when the user has answered all necessary data and submission of the registration succeeds" - {
        "redirect to the next page" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
          )
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          when(registrationService.amendRegistration(
            answers = any(),
            registration = any(),
            vrn = any(),
            iossNumber = any(),
            rejoin = ArgumentMatchers.eq(true))(any())) thenReturn Right(amendRegistrationResponse).toFuture

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              RejoinRegistrationPage.navigate(EmptyWaypoints, completeUserAnswersWithVatInfo, completeUserAnswersWithVatInfo).route.url
          }
        }
      }

      "when the user has answered all necessary data but submission of the registration fails" - {

        "when the exclusion does is not eligible" in {

          val registrationWrapperWithExclusionInFuture = createRegistrationWrapperWithExclusion(LocalDate.now().plusDays(1))

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionInFuture)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionInFuture)
          )
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionInFuture),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.CannotRejoinController.onPageLoad().url
          }
        }

        "redirect to error submitting amendment" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
          )
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          when(registrationService.amendRegistration(
            answers = any(),
            registration = any(),
            vrn = any(),
            iossNumber = any(),
            rejoin = ArgumentMatchers.eq(true)
          )(any())) thenReturn Left(InternalServerError).toFuture

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.ErrorSubmittingRejoinController.onPageLoad().url
          }
        }
      }

      "when the user has not answered all necessary data" - {

        "the page is refreshed when the incomplete prompt was not shown" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val application = applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
          )
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe rejoinRoutes.RejoinRegistrationController.onPageLoad().url
          }
        }
      }

      "the user is redirected when the incomplete prompt is shown" - {

        "to Has Trading Name when trading names are not populated correctly" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val application = applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
          )
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual controllers.tradingNames.routes.HasTradingNameController.onPageLoad(rejoinWaypoints).url
          }
        }

        "to Tax Registered In EU when it has a 'yes' answer but all countries were removed" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val rejoinRegistrationValidation = mock[RejoinRegistrationValidation]

          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          when(rejoinRegistrationValidation.validateEuRegistrations(
            ArgumentMatchers.eq(registrationWrapperWithExclusionOnBoundary),
            ArgumentMatchers.eq(rejoinWaypoints)
          )(any(), any(), any()))
            .thenReturn(Future.successful(Right(true)))

          when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Right(CurrentReturns(returns = Seq(), finalReturnsCompleted = true)).toFuture

          val answers = completeUserAnswers
            .set(TaxRegisteredInEuPage, true).success.value
            .set(EuCountryPage(Index(0)), country).success.value
            .remove(EuDetailsQuery(Index(0))).success.value

          val application = applicationBuilder(
            userAnswers = Some(answers),
            clock = Some(Clock.systemUTC()),
            registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
          )
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .overrides(bind[RejoinRegistrationValidation].toInstance(rejoinRegistrationValidation))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(rejoinWaypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual controllers.euDetails.routes.TaxRegisteredInEuController.onPageLoad(rejoinWaypoints).url
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

  private def getChangeRegistrationSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.row(answers, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(answers, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, rejoinWaypoints, rejoinRegistrationPage, lockEditing = false, isCurrentIossAccount)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, Seq.empty, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val taxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.row(answers, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(answers, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val websiteSummaryRow = WebsiteSummary.checkAnswersRow(answers, rejoinWaypoints, rejoinRegistrationPage, isCurrentIossAccount)
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(answers, rejoinWaypoints, rejoinRegistrationPage)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(answers, rejoinWaypoints, rejoinRegistrationPage)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(answers, rejoinWaypoints, rejoinRegistrationPage)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(answers, rejoinWaypoints, rejoinRegistrationPage)
    val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(answers, rejoinWaypoints, rejoinRegistrationPage)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(answers, rejoinWaypoints, rejoinRegistrationPage)

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
