/*
 * Copyright 2026 HM Revenue & Customs
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

/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import base.SpecBase
import connectors.RegistrationConnector
import controllers.actions.*
import models.CheckMode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.rejoin.RejoinRegistrationPage
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps
import views.html.EmailVerificationCodesAndEmailsExceededView

class EmailVerificationCodesAndEmailsExceededControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "EmailVerificationCodesAndEmailsExceeded Controller" - {

    "must return OK and the correct view for a GET" in {

      val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
      val mode: RegistrationModificationMode = NotModifyingExistingRegistration

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmailVerificationCodesAndEmailsExceededView]

        val redirectLink: String = "/business-account"

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(redirectLink, mode)(request, messages(application)).toString
      }
    }

    Seq(
      (AmendingActiveRegistration, ChangeRegistrationPage),
      (RejoiningRegistration, RejoinRegistrationPage),
      (AmendingPreviousRegistration, ChangePreviousRegistrationPage)
    ).foreach { (mode, page) =>

      s"must return OK and the correct view for a GET in $mode" in {

        when(mockRegistrationConnector.getRegistration()(any())) thenReturn Right(registrationWrapper).toFuture

        val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(page, CheckMode, page.urlFragment))
        val aMode: RegistrationModificationMode = mode

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EmailVerificationCodesAndEmailsExceededView]

          val redirectLink: String = page.route(waypoints).url

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(redirectLink, aMode)(request, messages(application)).toString
          verify(mockRegistrationConnector, times(1)).getRegistration()(any())
        }
      }
    }
  }
}
