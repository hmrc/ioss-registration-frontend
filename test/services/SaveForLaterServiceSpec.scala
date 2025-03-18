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

package services

import base.SpecBase
import connectors.{SavedUserAnswers, SaveForLaterConnector}
import controllers.routes
import models.requests.{AuthenticatedDataRequest, SaveForLaterRequest}
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{EmptyWaypoints, Waypoints}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SaveForLaterServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None, 1, None)
  implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), None, emptyUserAnswers, None, 1, None)

  private val waypoints: Waypoints = EmptyWaypoints

  private val mockSaveForLaterConnector = mock[SaveForLaterConnector]
  private val mockUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val saveForLaterService = new SaveForLaterService(mockUserAnswersRepository, mockSaveForLaterConnector)

  private lazy val redirectLocation = routes.BankDetailsController.onPageLoad(EmptyWaypoints)
  private lazy val originLocation = routes.BusinessContactDetailsController.onPageLoad(EmptyWaypoints)
  private lazy val errorLocation = routes.JourneyRecoveryController.onPageLoad()

  private val instantDate = Instant.now()
  private val saveForLaterRequest: SaveForLaterRequest = SaveForLaterRequest(vrn, Json.toJson("test"), None)
  private val savedUserAnswers: SavedUserAnswers = SavedUserAnswers(
    saveForLaterRequest.vrn,
    JsObject(Seq("saveForLaterRequest" -> Json.toJson(saveForLaterRequest.data))),
    Some(vatCustomerInfo),
    instantDate
  )
  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterConnector)
    Mockito.reset(mockUserAnswersRepository)
  }

  ".saveAnswers" - {

    "must Redirect to redirect location when answers are submitted successfully" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Right(Some(savedUserAnswers)))
      when(mockUserAnswersRepository.set(any())) thenReturn Future.successful(true)

      val result = saveForLaterService.saveAnswers(waypoints, redirectLocation, originLocation)

      result.futureValue mustBe Redirect(redirectLocation)

      verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
      verify(mockUserAnswersRepository, times(1)).set(any())
    }

    "must Redirect to error location when there is an unexpected result on submit" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Right(None))
      when(mockUserAnswersRepository.set(any())) thenReturn Future.successful(false)

      val result = saveForLaterService.saveAnswers(waypoints, redirectLocation, originLocation)

      result.futureValue mustBe Redirect(errorLocation)

      verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
      verifyNoInteractions(mockUserAnswersRepository)
    }

    "must Redirect to error location when an Unexpected Response Status is received" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Left(NotFound))
      when(mockUserAnswersRepository.set(any())) thenReturn Future.successful(false)

      val result = saveForLaterService.saveAnswers(waypoints, redirectLocation, originLocation)

      result.futureValue mustBe Redirect(errorLocation)

      verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
      verifyNoInteractions(mockUserAnswersRepository)
    }
  }

}
