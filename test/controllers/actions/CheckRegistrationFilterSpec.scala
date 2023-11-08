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
import config.FrontendAppConfig
import models.requests.AuthenticatedIdentifierRequest
import pages.filters.CannotRegisterAlreadyRegisteredPage
import pages.{EmptyWaypoints, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CheckRegistrationFilterSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints
  private val iossEnrolmentKey = "HMRC-IOSS-ORG"
  private val enrolment: Enrolment = Enrolment(iossEnrolmentKey, Seq.empty, "test", None)

  class Harness(inAmend: Boolean, config: FrontendAppConfig) extends CheckRegistrationFilterImpl(inAmend, config) {
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when an existing IOSS enrolment is not found" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(false, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return None when an existing IOSS enrolment is found and is inAmend" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None)
        val controller = new Harness(true, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return None when in amend" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None)
        val controller = new Harness(true, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must redirect to correct location when an existing IOSS enrolment is found" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None)
        val controller = new Harness(false, config)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(CannotRegisterAlreadyRegisteredPage.route(waypoints).url))
      }
    }
  }
}
