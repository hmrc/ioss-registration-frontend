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
import connectors.RegistrationConnector
import controllers.actions.{FakeIossRequiredAction, IossRequiredAction}
import controllers.rejoin.{routes => rejoinRoutes}
import models.{CheckMode, Index, UserAnswers}
import models.amend.RegistrationWrapper
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.etmp.amend.AmendRegistrationResponse
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.euDetails.{EuCountryPage, TaxRegisteredInEuPage}
import pages.rejoin.{CannotRejoinRegistrationPage, RejoinRegistrationPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import queries.euDetails.EuDetailsQuery
import services._
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.{VatRegistrationDetailsSummary, WebsiteSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, BusinessContactDetailsSummary}
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousRegistrations.{PreviouslyRegisteredSummary, PreviousRegistrationSummary}
import viewmodels.checkAnswers.tradingName.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.rejoin.RejoinRegistrationView

import java.time.{Clock, LocalDate, LocalDateTime}
import scala.concurrent.Future

class RejoinRegistrationControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinRegistrationPage, CheckMode, RejoinRegistrationPage.urlFragment))
  private val rejoinRegistrationPage = RejoinRegistrationPage
  private val registrationService = mock[RegistrationService]
  private val country = arbitraryCountry.arbitrary.sample.value

  private val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      iossReference = "IM900100000001",
      businessPartner = "businessPartner"
    )


  "RejoinRegistrationC Controller" - {

    ".onPageLoad" - {
      "must return OK and the correct view for a GET when the exclusion is valid" in {

        val registrationConnector = mock[RegistrationConnector]

        val registrationWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())
        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionOnBoundary)))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[IossRequiredAction]
            .toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWithExclusionOnBoundary)))
          .overrides(bind[RegistrationConnector]
            .toInstance(registrationConnector))
          .build()

        running(application) {

          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)

          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[RejoinRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))
          val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, vatInfoList, list, iossNumber, isValid = true)(request, messages(application)).toString
        }
      }

      "must error when the exclusion is invalid" in {

        val registrationConnector = mock[RegistrationConnector]

        val registrationWithExclusionInFuture = createRegistrationWrapperWithExclusion(LocalDate.now().plusDays(1))
        when(registrationConnector.getRegistration()(any()))
          .thenReturn(Future.successful(Right(registrationWithExclusionInFuture)))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
          .overrides(bind[IossRequiredAction]
            .toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWithExclusionInFuture)))
          .overrides(bind[RegistrationConnector]
            .toInstance(registrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, rejoinRoutes.RejoinRegistrationController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe CannotRejoinRegistrationPage.route(waypoints).url
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

      "when the user has answered all necessary data and submission of the registration succeeds" - {
        "redirect to the next page" in {

          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionOnBoundary)))
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          when(registrationService.amendRegistration(
            answers = any(),
            registration = any(),
            vrn = any(),
            iossNumber = any(),
            rejoin = ArgumentMatchers.eq(true))(any())) thenReturn Right(amendRegistrationResponse).toFuture

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
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
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionInFuture)))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionInFuture)))
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.CannotRejoinController.onPageLoad().url
          }
        }

        "redirect to error submitting amendment" in {
          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionOnBoundary)))
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          when(registrationService.amendRegistration(
            answers = any(),
            registration = any(),
            vrn = any(),
            iossNumber = any(),
            rejoin = ArgumentMatchers.eq(true)
          )(any())) thenReturn Left(InternalServerError).toFuture

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
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
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionOnBoundary)))
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = false).url)
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
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionOnBoundary)))
            .overrides(bind[RegistrationService].toInstance(registrationService))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual controllers.tradingNames.routes.HasTradingNameController.onPageLoad(waypoints).url
          }
        }

        "to Tax Registered In EU when it has a 'yes' answer but all countries were removed" in {
          val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

          val registrationConnector = mock[RegistrationConnector]
          val registrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)
          when(registrationConnector.getRegistration()(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))

          val answers = completeUserAnswers
            .set(TaxRegisteredInEuPage, true).success.value
            .set(EuCountryPage(Index(0)), country).success.value
            .remove(EuDetailsQuery(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(answers), clock = Some(Clock.systemUTC()))
            .overrides(bind[IossRequiredAction].toInstance(new FakeIossRequiredAction(Some(completeUserAnswersWithVatInfo), registrationWrapperWithExclusionOnBoundary)))
            .overrides(bind[RegistrationConnector].toInstance(registrationConnector))
            .build()

          running(application) {
            val request = FakeRequest(POST, rejoinRoutes.RejoinRegistrationController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual controllers.euDetails.routes.TaxRegisteredInEuController.onPageLoad(waypoints).url
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

    val hasTradingNameSummaryRow = HasTradingNameSummary.row(answers, waypoints, rejoinRegistrationPage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(answers, waypoints, rejoinRegistrationPage)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, rejoinRegistrationPage, lockEditing = false)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, Seq.empty, waypoints, rejoinRegistrationPage)
    val taxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.row(answers, waypoints, rejoinRegistrationPage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(answers, waypoints, rejoinRegistrationPage)
    val websiteSummaryRow = WebsiteSummary.checkAnswersRow(answers, waypoints, rejoinRegistrationPage)
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowContactName(answers, waypoints, rejoinRegistrationPage)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(answers, waypoints, rejoinRegistrationPage)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(answers, waypoints, rejoinRegistrationPage)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(answers, waypoints, rejoinRegistrationPage)
    val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(answers, waypoints, rejoinRegistrationPage)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(answers, waypoints, rejoinRegistrationPage)

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
