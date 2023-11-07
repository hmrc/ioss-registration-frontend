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
import models.requests.{AuthenticatedIdentifierRequest, AuthenticatedIdentifierRequestWithMandatoryIossReference}
import play.api.mvc.Result
import play.api.mvc.Results.Unauthorized
import play.api.test.FakeRequest
import play.api.test.Helpers.GET

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IossRequiredActionSpec extends SpecBase {

  class Harness() extends IossRequiredAction {

    def callRefine[A](request: AuthenticatedIdentifierRequest[A]):
    Future[Either[Result, AuthenticatedIdentifierRequestWithMandatoryIossReference[A]]] = refine(request)
  }

  "Ioss Required Action" - {

    "when the user has logged in as an Organisation Admin with strong credentials but ioss enrolment" - {

      "must return Unauthorized" in {

        val action = new Harness()
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthenticatedIdentifierRequest(
          request,
          testCredentials,
          vrn,
          enrolments,
          None
        )).futureValue

        result mustBe Left(Unauthorized)
      }

      "must return Right" in {

        val action = new Harness()
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthenticatedIdentifierRequest(
          request,
          testCredentials,
          vrn,
          enrolments,
          Some(iossNumber)
        )).futureValue

        val expectResult = AuthenticatedIdentifierRequestWithMandatoryIossReference(request, testCredentials, vrn, enrolments, iossNumber)

        result mustBe Right(expectResult)
      }
    }
  }
}
