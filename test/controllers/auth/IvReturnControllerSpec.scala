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
import controllers.auth.{routes => authRoutes}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.iv._

class IvReturnControllerSpec extends SpecBase {

  private val continueUrl: String = "continueUrl"

  "IvReturn Controller" - {

    ".error" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.error().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvErrorView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".incomplete" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.incomplete().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvIncompleteView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".insufficientEvidence" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.insufficientEvidence().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[InsufficientEvidenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".lockedOut" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.lockedOut().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvLockedOutView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".preconditionFailed" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.preconditionFailed().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvPreconditionFailedView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".technicalIssue" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.technicalIssue().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvTechnicalIssueView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".timeout" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.timeout().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvTimeoutView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".userAborted" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.userAborted(continueUrl).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvUserAbortedView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }
    }

    ".notEnoughEvidenceSources" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.notEnoughEvidenceSources().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvNotEnoughEvidenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    ".failedMatching" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.failedMatching(continueUrl).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvFailedMatchingView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }
    }

    ".failed" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, authRoutes.IvReturnController.failed(continueUrl).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IvFailedView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }
    }
  }
}
