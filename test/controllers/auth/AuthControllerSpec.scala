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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.auth.{routes => authRoutes}
import models.{UserAnswers, VatApiCallResult, responses}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CheckVatDetailsPage, EmptyWaypoints, VatApiDownPage, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.VatApiCallResultQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.net.URLEncoder

class AuthControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]

  private val continueUrl: String = "continueUrl"
  private val waypoints: Waypoints = EmptyWaypoints

  private def appBuilder(answers: Option[UserAnswers]) =
    applicationBuilder(answers)
      .overrides(
        bind[RegistrationConnector].toInstance(mockRegistrationConnector),
        bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
  )

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuthenticatedUserAnswersRepository)
  }

  ".onSignIn" - {

    "when we already have some user answers" - {

      "and we have a made a call to get VAT info" - {

        "must redirect to the next page without making calls to get data or updating the users answers" in {

          val answers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

          val application = appBuilder(Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url
            verifyNoInteractions(mockRegistrationConnector)
            verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
          }
        }
      }

      "and we have not yet made a call to get VAT info" - {

        "and we can find their VAT details" - {

          "must create user answers with their VAT details, then redirect to the next page" in {

            val application = appBuilder(Some(emptyUserAnswers)).build()

            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers = emptyUserAnswersWithVatInfo.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }

        "and we cannot find their VAT details" - {

          "must redirect to VAT API down page" in {

            val application = appBuilder(Some(emptyUserAnswers)).build()

            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(responses.NotFound).toFuture
            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }

        "and the call to get their VAT details fails" - {

          val failureResponse = responses.UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")

          "must return an internal server error" in {

            val application = appBuilder(None).build()

            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(failureResponse).toFuture
            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }
      }
    }

    "when we don't already have some user answers" - {

      "and we can find their VAT details" - {

        "must create user answers with their VAT details, then redirect to the next page" in {

          val application = appBuilder(None).build()

          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {
            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswersWithVatInfo.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url
            verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
          }
        }
      }

      "and we cannot find their VAT details" - {

        "must redirect to VAT API down page" in {

          val application = appBuilder(None).build()

          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(responses.NotFound).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {

            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url
            verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
          }
        }

      }

      "and the call to get their vat details fails" - {

        val failureResponse = responses.UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")

        "must return an internal server error" in {

          val application = appBuilder(None).build()

          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(failureResponse).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {

            val request = FakeRequest(GET, routes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url
            verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
          }
        }
      }
    }
  }

  ".redirectToRegister" - {

    "must redirect the user to bas-gateway to register" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.redirectToRegister(continueUrl).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual "http://localhost:9553/bas-gateway/register?origin=IOSS&continueUrl=continueUrl&accountType=Organisation"
      }
    }
  }

  ".redirectToLogin" - {

    "must redirect the user to bas-gateway to log in" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.redirectToLogin(continueUrl).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual "http://localhost:9553/bas-gateway/sign-in?origin=IOSS&continue=continueUrl"
      }
    }
  }

  ".signOut" - {

    "must redirect to sign out, specifying the exit survey as the continue URL" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOut.url)

        val result = route(application, request).value

        val encodedContinueUrl = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }

  ".signOutNoSurvey" - {

    "must redirect to sign out, specifying SignedOut as the continue URL" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOutNoSurvey.url)

        val result = route(application, request).value

        val encodedContinueUrl = URLEncoder.encode(routes.SignedOutController.onPageLoad.url, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }

  ".unsupportedAffinityGroup" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.unsupportedAffinityGroup().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnsupportedAffinityGroupView]

        status(result) mustBe OK

        contentAsString(result) mustBe view()(request, messages(application)).toString()
      }
    }

  }

  ".unsupportedAuthProvider" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.unsupportedAuthProvider(continueUrl).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnsupportedAuthProviderView]

        status(result) mustBe OK

        contentAsString(result) mustBe view(continueUrl)(request, messages(application)).toString()
      }
    }
  }

  ".insufficientEnrolments" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.insufficientEnrolments().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InsufficientEnrolmentsView]

        status(result) mustBe OK

        contentAsString(result) mustBe view()(request, messages(application)).toString()
      }
    }
  }

  ".unsupportedCredentialRole" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.unsupportedCredentialRole().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnsupportedCredentialRoleView]

        status(result) mustBe OK

        contentAsString(result) mustBe view()(request, messages(application)).toString()
      }
    }
  }
}
