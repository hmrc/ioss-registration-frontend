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
import models.domain.VatCustomerInfo
import models.{DesAddress, UserAnswers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.filters.RegisteredForIossInEuPage
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
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

  val arbitraryDate: LocalDate = datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31)).sample.value
  val arbitraryInstant: Instant = arbitraryDate.atStartOfDay(ZoneId.systemDefault()).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault())

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = DesAddress("Line1", None, None, None, None, Some("AA11 1AA"), "GB"),
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = Some(true),
      deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate))
    )

  val vrn: Vrn = Vrn("123456789")

  val userAnswersId: String = "12345-credId"

  def testCredentials: Credentials = Credentials(userAnswersId, "GGW")
  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId, lastUpdated = arbitraryInstant)
  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))
  def basicUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.set(RegisteredForIossInEuPage, false).success.value.copy(vatInfo = Some(vatCustomerInfo))


  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None, clock: Option[Clock] = None): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthenticatedIdentifierAction].to[FakeAuthenticatedIdentifierAction],
        bind[AuthenticatedDataRetrievalAction].toInstance(new FakeAuthenticatedDataRetrievalAction(userAnswers, vrn)),
        bind[UnauthenticatedDataRetrievalAction].toInstance(new FakeUnauthenticatedDataRetrievalAction(userAnswers)),
        bind[AuthenticatedDataRequiredActionImpl].toInstance(FakeAuthenticatedDataRequiredAction(userAnswers)),
        bind[Clock].toInstance(clockToBind)
      )
  }

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
}
