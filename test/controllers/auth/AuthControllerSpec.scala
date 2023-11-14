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
import connectors.{RegistrationConnector, SaveForLaterConnector, SavedUserAnswers}
import controllers.auth.{routes => authRoutes}
import models.{UserAnswers, VatApiCallResult, responses}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.checkVatDetails.CheckVatDetailsPage
import pages.filters.{BusinessBasedInNiPage, NorwegianBasedBusinessPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.VatApiCallResultQuery
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.net.URLEncoder
import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val mockSavedAnswersConnector = mock[SaveForLaterConnector]

  private val continueUrl = "http://localhost/foo"
  private val waypoints: Waypoints = EmptyWaypoints

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuthenticatedUserAnswersRepository)
    reset(mockSavedAnswersConnector)
  }

  private def appBuilder(answers: Option[UserAnswers]) =
    applicationBuilder(answers)
      .overrides(
        bind[RegistrationConnector].toInstance(mockRegistrationConnector),
        bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository),
        bind[SaveForLaterConnector].toInstance(mockSavedAnswersConnector)
      )

  ".onSignIn" - {

    "when we already have some user answers" - {

      "must redirect to the ContinueRegistration page if saved url was retrieved from saved answers" in {
        val answers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value
          .set(SavedProgressPage, "/url").success.value

        val application = appBuilder(Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.AuthController.onSignIn().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.ContinueRegistrationController.onPageLoad().url

          verifyNoInteractions(mockRegistrationConnector)
          verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
          verifyNoInteractions(mockSavedAnswersConnector)
        }
      }

      "and we have a made a call to get VAT info" - {

        "must redirect to the next page without making calls to get data or updating the users answers" in {
          val answers = emptyUserAnswersWithVatInfo.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

          when(mockSavedAnswersConnector.get()(any()))
            .thenReturn(Future.successful(Right(None)))

          val application = appBuilder(Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

            verify(mockSavedAnswersConnector, times(1)).get()(any())
            verifyNoInteractions(mockRegistrationConnector)
            verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
          }
        }
      }

      "and we have not yet made a call to get VAT info" - {

        "and we can find their VAT details" - {

          "and the de-registration date is today or before" - {

            "must redirect to Expired Vrn Date page" in {
              val application = appBuilder(answers = Some(emptyUserAnswersWithVatInfo)).build()
              val nonExpiredVrnVatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate)))

              when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(nonExpiredVrnVatInfo).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                status(result) mustBe SEE_OTHER

                verify(mockSavedAnswersConnector, times(1)).get()(any())
                redirectLocation(result).value mustBe ExpiredVrnDatePage.route(waypoints).url
                verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
              }
            }
          }

          "and the de-registration date is later than today" - {

            "must create user answers with their VAT details, then redirect to the next page" in {
              val answers = emptyUserAnswersWithVatInfo.set(BusinessBasedInNiPage, true).success.value
              val application = appBuilder(answers = Some(answers)).build()

              val nonExpiredVrnVatInfo = vatCustomerInfo.copy(
                singleMarketIndicator = true,
                deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate).plusDays(1))
              )

              when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(nonExpiredVrnVatInfo).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                val expectedAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(nonExpiredVrnVatInfo))
                  .set(BusinessBasedInNiPage, true).success.value
                  .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                status(result) mustBe SEE_OTHER

                verify(mockSavedAnswersConnector, times(1)).get()(any())
                redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url
                verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
              }
            }
          }

          "and the user answers that they are registered to trade under the NI protocol but ETMP indicate they are not" - {

            "must redirect to the Cannot Register No Ni Protocol page" in {

              val updatedVatInfo = vatCustomerInfo
                .copy(singleMarketIndicator = false)
                .copy(overseasIndicator = true)

              val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                .set(BusinessBasedInNiPage, true).success.value

              val application = appBuilder(answers = Some(answers)).build()

              when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                status(result) mustBe SEE_OTHER
                redirectLocation(result).value mustBe CannotRegisterNoNiProtocolPage.route(waypoints).url

                verify(mockSavedAnswersConnector, times(1)).get()(any())
                verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
              }
            }
          }

          // TODO users answered no in filter Qs to NI question, and yes to Norway but ETMP shows NI true and No Norway so should be able to register
          //          "and the user answers that they are not registered to trade under the NI protocol but ETMP indicate they are" - {
          //
          //            "must redirect to the Cannot Register No Ni Protocol page" in {
          //
          //              val answers = emptyUserAnswersWithVatInfo
          //                .set(BusinessBasedInNiPage, false).success.value
          //                .set(NorwegianBasedBusinessPage, true).success.value
          //
          //              val application = appBuilder(answers = Some(answers)).build()
          //
          //              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
          //              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture
          //
          //              running(application) {
          //
          //                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
          //                val result = route(application, request).value
          //
          //                status(result) mustBe SEE_OTHER
          //                redirectLocation(result).value mustBe CannotRegisterNoNiProtocolPage.route(waypoints).url
          //                verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
          //              }
          //            }
          //          }

          "they are registered to trade under the NI protocol" - {

            "must create user answers with their VAT details, then redirect to the next page" in {

              val answers = emptyUserAnswersWithVatInfo.set(BusinessBasedInNiPage, true).success.value

              val application = appBuilder(answers = Some(answers)).build()

              when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                val expectedAnswers = answers
                  .set(BusinessBasedInNiPage, true).success.value
                  .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                status(result) mustBe SEE_OTHER
                redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

                verify(mockSavedAnswersConnector, times(1)).get()(any())
                verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
              }
            }
          }

          "and they are not registered to trade under the NI protocol" - {

            "and they answer that their business is Norway based" - {

              "but ETMP indicate their registered PPOB is not Norway" - {

                "must redirect to the Cannot Register Not Norwegian Based Business page" in {

                  val updatedVatInfo = vatCustomerInfo
                    .copy(singleMarketIndicator = false)
                    .copy(overseasIndicator = true)

                  val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                    .set(BusinessBasedInNiPage, false).success.value
                    .set(NorwegianBasedBusinessPage, true).success.value

                  val application = appBuilder(answers = Some(answers)).build()

                  when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                  when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                  when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

                  running(application) {

                    val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                    val result = route(application, request).value

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).value mustBe CannotRegisterNotNorwegianBasedBusinessPage.route(waypoints).url

                    verify(mockSavedAnswersConnector, times(1)).get()(any())
                    verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
                  }
                }
              }

              "and ETMP indicate their registered PPOB is Norway" - {

                "must create user answers with their VAT details, then redirect to the next page" in {

                  val desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(countryCode = "NO")
                  val updatedVatInfo = vatCustomerInfo.copy(
                    singleMarketIndicator = false,
                    desAddress = desAddress
                  )

                  val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                    .set(BusinessBasedInNiPage, false).success.value
                    .set(NorwegianBasedBusinessPage, true).success.value

                  val application = appBuilder(answers = Some(answers)).build()

                  when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                  when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                  when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

                  running(application) {

                    val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                    val result = route(application, request).value

                    val expectedAnswers = answers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

                    verify(mockSavedAnswersConnector, times(1)).get()(any())
                    verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                  }
                }
              }
            }
          }

          "and the user is a Non Established Taxable Person" - {
            "but the user is a single market" - {
              "must redirect to the Cannot Register Non Established Taxable Person Page" in {
                val updatedVatInfo = vatCustomerInfo
                  .copy(singleMarketIndicator = true)
                  .copy(overseasIndicator = true)

                val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                  .set(BusinessBasedInNiPage, true).success.value

                val application = appBuilder(answers = Some(answers)).build()

                when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

                running(application) {

                  val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                  val result = route(application, request).value

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).value mustBe CannotRegisterNonEstablishedTaxablePersonPage.route(waypoints).url

                  verify(mockSavedAnswersConnector, times(1)).get()(any())
                  verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
                }
              }
            }

            "but the user business is based in Norway" - {
              "must then redirect to the next page" in {

                val desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(countryCode = "NO")

                val updatedVatInfo = vatCustomerInfo
                  .copy(
                    singleMarketIndicator = false,
                    overseasIndicator = true,
                    desAddress = desAddress
                  )

                val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                  .set(BusinessBasedInNiPage, false).success.value
                  .set(NorwegianBasedBusinessPage, true).success.value

                val application = appBuilder(answers = Some(answers)).build()

                when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

                running(application) {

                  val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                  val result = route(application, request).value

                  val expectedAnswers = answers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

                  verify(mockSavedAnswersConnector, times(1)).get()(any())
                  verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                }
              }
            }

            "when the user is Non Established Taxable Person or single market" - {
              "must redirect to the Cannot Register Non Established Taxable Person Page" in {
                val updatedVatInfo = vatCustomerInfo
                  .copy(overseasIndicator = true)

                val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                  .set(BusinessBasedInNiPage, true).success.value

                val application = appBuilder(answers = Some(answers)).build()

                when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

                running(application) {

                  val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                  val result = route(application, request).value

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).value mustBe CannotRegisterNonEstablishedTaxablePersonPage.route(waypoints).url

                  verify(mockSavedAnswersConnector, times(1)).get()(any())
                  verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
                }
              }

              "but the user business is based in Norway" - {
                "must then redirect to the next page" in {

                  val desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(countryCode = "NO")

                  val updatedVatInfo = vatCustomerInfo
                    .copy(
                      singleMarketIndicator = false,
                      overseasIndicator = false,
                      desAddress = desAddress
                    )

                  val answers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(updatedVatInfo))
                    .set(BusinessBasedInNiPage, false).success.value
                    .set(NorwegianBasedBusinessPage, true).success.value

                  val application = appBuilder(answers = Some(answers)).build()

                  when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
                  when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(updatedVatInfo).toFuture
                  when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

                  running(application) {

                    val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                    val result = route(application, request).value

                    val expectedAnswers = answers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

                    verify(mockSavedAnswersConnector, times(1)).get()(any())
                    verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                  }
                }
              }
            }

          }

        }

        "and we cannot find their VAT details" - {

          "must redirect to VAT API down page" in {

            val application = appBuilder(Some(emptyUserAnswers)).build()

            when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(responses.NotFound).toFuture
            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url

              verify(mockSavedAnswersConnector, times(1)).get()(any())
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }

        "and the call to get their VAT details fails" - {

          val failureResponse = responses.UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")

          "must return an internal server error" in {

            val application = appBuilder(None).build()

            when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(failureResponse).toFuture
            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url

              verify(mockSavedAnswersConnector, times(1)).get()(any())
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }
      }
    }

    "when we don't already have some user answers" - {

      "and we can find their VAT details" - {

        "must create user answers with their VAT details, then redirect to the next page" in {

          val application = appBuilder(answers = Some(emptyUserAnswers
            .set(BusinessBasedInNiPage, true).success.value))
            .build()

          when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {
            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswersWithVatInfo
              .set(BusinessBasedInNiPage, true).success.value
              .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url

            verify(mockSavedAnswersConnector, times(1)).get()(any())
            verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
          }
        }
      }

      "and we cannot find their VAT details" - {

        "must redirect to VAT API down page" in {

          val application = appBuilder(None).build()

          when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(responses.NotFound).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {

            val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url

            verify(mockSavedAnswersConnector, times(1)).get()(any())
            verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
          }
        }
      }

      "and the call to get their vat details fails" - {

        val failureResponse = responses.UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")

        "must return an internal server error" in {

          val application = appBuilder(None).build()

          when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(failureResponse).toFuture
          when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

          running(application) {

            val request = FakeRequest(GET, routes.AuthController.onSignIn().url)
            val result = route(application, request).value

            val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url

            verify(mockSavedAnswersConnector, times(1)).get()(any())
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
        val request = FakeRequest(GET, routes.AuthController.redirectToRegister(RedirectUrl("http://localhost/foo")).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustEqual "http://localhost:9553/bas-gateway/register?origin=IOSS&continueUrl=http%3A%2F%2Flocalhost%2Ffoo&accountType=Organisation"
      }
    }
  }

  ".redirectToLogin" - {

    "must redirect the user to bas-gateway to log in" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.redirectToLogin(RedirectUrl("http://localhost/foo")).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustEqual "http://localhost:9553/bas-gateway/sign-in?origin=IOSS&continue=http%3A%2F%2Flocalhost%2Ffoo"
      }
    }
  }

  "continueOnSignIn" - {

    "must redirect to the ContinueRegistration page if saved url was retrieved from saved answers" in {

      val answers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value
        .set(SavedProgressPage, "/url").success.value

      val application = applicationBuilder(Some(answers)).build()
      when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(Some(SavedUserAnswers(vrn, answers.data, None, Instant.now))))

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.continueOnSignIn().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.ContinueRegistrationController.onPageLoad().url
      }
    }

    "must redirect to NoRegistrationInProgress when there is no saved answers" in {

      val application = applicationBuilder(None).build()
      when(mockSavedAnswersConnector.get()(any())) thenReturn Future.successful(Right(None))

      running(application) {
        val request = FakeRequest(GET, routes.AuthController.continueOnSignIn().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.NoRegistrationInProgressController.onPageLoad().url
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

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirectUrl
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

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedRedirectUrl
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

        val request = FakeRequest(GET, routes.AuthController.unsupportedAuthProvider(RedirectUrl("http://localhost/foo")).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnsupportedAuthProviderView]

        status(result) mustBe OK

        contentAsString(result) mustBe view(RedirectUrl(continueUrl))(request, messages(application)).toString
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
