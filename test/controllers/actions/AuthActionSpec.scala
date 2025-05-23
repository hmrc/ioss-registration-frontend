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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.auth.routes as authRoutes
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.oss.OssRegistrationService
import services.{AccountService, UrlBuilderService}
import testutils.TestAuthRetrievals.Ops
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.bootstrap.binders.OnlyRelative
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import utils.FutureSyntax.FutureOps

import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel
  private val vatEnrolment: Enrolments = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatDecEnrolment: Enrolments = Enrolments(Set(Enrolment("HMCE-VATDEC-ORG", Seq(EnrolmentIdentifier("VATRegNo", "123456789")), "Activated")))

  class Harness(authAction: AuthenticatedIdentifierAction, defaultAction: DefaultActionBuilder) {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockAccountService: AccountService = mock[AccountService]
  val mockOssRegistrationService: OssRegistrationService = mock[OssRegistrationService]

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAccountService)
    reset(mockOssRegistrationService)
  }

  "Auth Action" - {

    "when the user is logged in as an Organisation Admin with a VAT enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L250).toFuture

          when(mockOssRegistrationService.getLatestOssRegistration(any(), any())(any())) thenReturn ossRegistration.toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe OK
        }
      }
    }

    "when the user is logged in as an Organisation Admin with a VATDEC enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ vatDecEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L250).toFuture

          when(mockOssRegistrationService.getLatestOssRegistration(any(), any())(any())) thenReturn ossRegistration.toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe OK
        }
      }
    }

    "when the user is logged in as an Individual with a VAT enrolment, strong credentials and confidence level 250" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L250).toFuture

          when(mockOssRegistrationService.getLatestOssRegistration(any(), any())(any())) thenReturn ossRegistration.toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe OK
        }
      }
    }

    "when the user has logged in as an Organisation Admin with strong credentials but no vat enrolment" - {

      "must be redirected to the Insufficient Enrolments page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Organisation) ~ ConfidenceLevel.L50).toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe authRoutes.AuthController.insufficientEnrolments().url
        }
      }
    }

    "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less than 250" - {

      "must be redirected to uplift their confidence level" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L50).toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(s"${appConfig.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
        }
      }
    }

    "when the user has logged in as an Individual without a VAT enrolment" - {

      "must be redirected to Insufficient Enrolments page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Individual) ~ ConfidenceLevel.L250).toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe authRoutes.AuthController.insufficientEnrolments().url
        }
      }
    }

    "when the user has logged in as an Individual with no credentials" - {

      "must redirect the user to the Unauthorised page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())) thenReturn
            (None ~ Enrolments(Set.empty) ~ Some(Individual) ~ ConfidenceLevel.L50).toFuture

          val action = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            appConfig,
            urlBuilder,
            mockAccountService,
            mockOssRegistrationService
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest("", "/endpoint"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
          val controller = new Harness(authAction, actionBuilder)
          val request = FakeRequest("", "/endpoint")
          val result = controller.onPageLoad()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.loginUrl + "?continue=" + URLEncoder.encode(urlBuilder.loginContinueUrl(request).get(OnlyRelative).url, "UTF-8")
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unsupported auth provider page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            appConfig,
            urlBuilder,
            mockAccountService,
            mockOssRegistrationService
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest("", "/endpoint"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.auth.routes.AuthController.unsupportedAuthProvider(urlBuilder.loginContinueUrl(FakeRequest("", "/endpoint"))).url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unsupported affinity group page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            appConfig,
            urlBuilder,
            mockAccountService,
            mockOssRegistrationService
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe authRoutes.AuthController.unsupportedAffinityGroup().url
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            appConfig,
            urlBuilder,
            mockAccountService,
            mockOssRegistrationService
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe authRoutes.AuthController.unsupportedCredentialRole().url
        }
      }
    }

    "the user has weak credentials" - {

      "must redirect the user to an MFA uplift journey" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new IncorrectCredentialStrength()),
            appConfig,
            urlBuilder,
            mockAccountService,
            mockOssRegistrationService
          )
          val controller = new Harness(authAction, actionBuilder)
          val request = FakeRequest().withHeaders(HeaderNames.xSessionId -> "123")
          val result = controller.onPageLoad()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe "http://localhost:9553/bas-gateway/uplift-mfa?origin=IOSS&continueUrl=http%3A%2F%2Flocalhost%3A10190%2F%3Fk%3D123"
        }
      }
    }

    "the connector returns other AuthorizationException" - {

      val exceptions = List(InternalError(), FailedRelationship(), IncorrectNino)

      exceptions.foreach { e =>
        s"$e must redirect the user to an unauthorised page" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val appConfig = application.injector.instanceOf[FrontendAppConfig]
            val urlBuilder = application.injector.instanceOf[UrlBuilderService]
            val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

            val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(e), appConfig, urlBuilder, mockAccountService, mockOssRegistrationService)
            val controller = new Harness(authAction, actionBuilder)
            val request = FakeRequest().withHeaders(HeaderNames.xSessionId -> "123")
            val result = controller.onPageLoad()(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
          }
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
