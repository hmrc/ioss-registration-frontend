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
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckAmendPreviousRegistrationFilterSpec extends SpecBase {

  class Harness(registrationModificationMode: ModifyingExistingRegistrationMode, restrictFromPreviousRegistrations: Boolean, config: FrontendAppConfig)
    extends CheckAmendPreviousRegistrationFilterImpl(registrationModificationMode, restrictFromPreviousRegistrations, config) {
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when registrationModificationMode is AmendingPreviousRegistration and restrictFromPreviousRegistrations is false" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, 1, None)
        val controller = new Harness(registrationModificationMode = AmendingPreviousRegistration, restrictFromPreviousRegistrations = false, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must redirect to Journey Recovery Page for anything else" in {

      val app = applicationBuilder(None).build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, 1, None)
        val controller = new Harness(registrationModificationMode = AmendingPreviousRegistration, restrictFromPreviousRegistrations = true, config)

        Some(RedirectUrl(config.iossYourAccountUrl))
        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(config.iossYourAccountUrl))
      }
    }
  }
}
