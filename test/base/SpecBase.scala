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

package base

import controllers.actions._
import generators.Generators
import models.amend.RegistrationWrapper
import models.domain.VatCustomerInfo
import models.emailVerification.{EmailVerificationRequest, VerifyEmail}
import models.{BankDetails, Bic, BusinessContactDetails, CheckMode, Iban, Index, UserAnswers, Website}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.euDetails.TaxRegisteredInEuPage
import pages.filters.RegisteredForIossInEuPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.website.WebsitePage
import pages.{BankDetailsPage, BusinessContactDetailsPage, CheckAnswersPage, EmptyWaypoints, NonEmptyWaypoints, Waypoint}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with Generators {

  val arbitraryInstant: Instant = arbitraryDate.arbitrary.sample.value.atStartOfDay(ZoneId.systemDefault()).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault())

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val contactDetails: BusinessContactDetails = BusinessContactDetails("name", "0111 2223334", "email@example.com")
  val amendedContactDetails: BusinessContactDetails = BusinessContactDetails("name", "0111 2223334", "email@example.co.uk")

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = arbitraryDesAddress.arbitrary.sample.value,
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None,
      overseasIndicator = false
    )

  val vrn: Vrn = Vrn("123456789")
  val iossNumber: String = "IM9001234567"

  val userAnswersId: String = "12345-credId"

  val iban: Iban = Iban("GB33BUKB20201555555555").value
  val bic: Bic = Bic("ABCDGB2A").get

  val completeUserAnswers: UserAnswers = basicUserAnswersWithVatInfo
    .set(HasTradingNamePage, false).success.value
    .set(TaxRegisteredInEuPage, false).success.value
    .set(PreviouslyRegisteredPage, false).success.value

  def testCredentials: Credentials = Credentials(userAnswersId, "GGW")
  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId, lastUpdated = arbitraryInstant)
  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))
  def basicUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.set(RegisteredForIossInEuPage, false).success.value.copy(vatInfo = Some(vatCustomerInfo))

  def completeUserAnswersWithVatInfo: UserAnswers =
    basicUserAnswersWithVatInfo
      .set(HasTradingNamePage, false).success.value
      .set(PreviouslyRegisteredPage, false).success.value
      .set(TaxRegisteredInEuPage, false).success.value
      .set(WebsitePage(Index(0)), Website("www.test-website.com")).success.value
      .set(BusinessContactDetailsPage, BusinessContactDetails("fullName", "0123456789", "testEmail@example.com")).success.value
      .set(BankDetailsPage, BankDetails("Account name", Some(bic), iban)).success.value

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    clock: Option[Clock] = None,
                                    registrationWrapper: Option[RegistrationWrapper] = None
                                  ): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthenticatedIdentifierAction].to[FakeAuthenticatedIdentifierAction],
        bind[AuthenticatedDataRetrievalAction].toInstance(new FakeAuthenticatedDataRetrievalAction(userAnswers, vrn)),
        bind[UnauthenticatedDataRetrievalAction].toInstance(new FakeUnauthenticatedDataRetrievalAction(userAnswers)),
        bind[AuthenticatedDataRequiredActionImpl].toInstance(FakeAuthenticatedDataRequiredAction(userAnswers)),
        bind[CheckOtherCountryRegistrationFilter].toInstance(new FakeCheckOtherCountryRegistrationFilter()),
        bind[CheckRegistrationFilterProvider].toInstance(new FakeCheckRegistrationFilterProvider()),
        bind[CheckEmailVerificationFilterProvider].toInstance(new FakeCheckEmailVerificationFilter()),
        bind[SavedAnswersRetrievalActionProvider].toInstance(new FakeSavedAnswersRetrievalActionProvider(userAnswers, vrn)),
        bind[CheckBouncedEmailFilterProvider].toInstance(new FakeCheckBouncedEmailFilterProvider()),
        bind[IossRequiredActionImpl].toInstance(new FakeIossRequiredAction(userAnswers, registrationWrapper.getOrElse(this.registrationWrapper))()),
        bind[CheckAmendPreviousRegistrationFilterProvider].toInstance(new FakeCheckAmendPreviousRegistrationFilterProvider()),
        bind[Clock].toInstance(clockToBind)
      )
  }

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "/endpoint").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  val verifyEmail: VerifyEmail = VerifyEmail(
    address = contactDetails.emailAddress,
    enterUrl = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
  )

  val emailVerificationRequest: EmailVerificationRequest = EmailVerificationRequest(
    credId = userAnswersId,
    continueUrl = "/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop/bank-details",
    origin = "IOSS",
    deskproServiceName = Some("one-stop-shop-registration-frontend"),
    accessibilityStatementUrl = "/register-and-pay-vat-on-goods-sold-to-eu-from-northern-ireland",
    pageTitle = Some("VAT Import One Stop Shop scheme"),
    backUrl = Some("/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"),
    email = Some(verifyEmail)
  )

  val yourAccountUrl = "http://localhost:10193/pay-vat-on-goods-sold-to-eu/import-one-stop-shop-returns-payments"
  val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

  protected def createCheckModeWayPoint(checkAnswersPage: CheckAnswersPage): NonEmptyWaypoints =
    EmptyWaypoints.setNextWaypoint(Waypoint(checkAnswersPage, CheckMode, checkAnswersPage.urlFragment))

}
