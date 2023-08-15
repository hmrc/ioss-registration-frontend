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
import controllers.routes
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UrlBuilderService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(authAction: AuthenticatedIdentifierAction, defaultAction: DefaultActionBuilder) {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder    = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

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

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
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

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
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
