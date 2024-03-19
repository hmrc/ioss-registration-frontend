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
import models.requests.AuthenticatedIdentifierRequest
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CheckAmendPreviousRegistrationFilterSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints

  class Harness(registrationModificationMode: ModifyingExistingRegistrationMode, restrictFromPreviousRegistrations: Boolean)
    extends CheckAmendPreviousRegistrationFilterImpl(registrationModificationMode, restrictFromPreviousRegistrations) {
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when registrationModificationMode is AmendingPreviousRegistration and restrictFromPreviousRegistrations is false" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(registrationModificationMode = AmendingPreviousRegistration, restrictFromPreviousRegistrations = false)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must redirect to Journey Recovery Page for anything else" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(registrationModificationMode = AmendingPreviousRegistration, restrictFromPreviousRegistrations = true)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(JourneyRecoveryPage.route(waypoints).url))
      }
    }
  }
}
