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
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest}
import models.{BusinessContactDetails, CheckMode}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{BusinessContactDetailsPage, EmptyWaypoints, Waypoint}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckBouncedEmailFilterSpec extends SpecBase with MockitoSugar {

  class Harness extends CheckBouncedEmailFilterImpl() {
    def callFilter(request: AuthenticatedMandatoryIossRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val authDataRequest = AuthenticatedDataRequest(
    FakeRequest(),
    testCredentials,
    vrn,
    Some(iossNumber),
    completeUserAnswers,
    Some(registrationWrapper)
  )

  ".filter" - {

    "when the unusable email status is true" - {

      "and email is the same as answers" - {

        "must redirect to Intercept Unusable Email" in {

          val app = applicationBuilder(None)
            .build()

          val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
            registrationWrapper.registration.copy(schemeDetails =
              registrationWrapper.registration.schemeDetails.copy(unusableStatus = true)))

          val changeRegWaypoint = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

          running(app) {

            val request = AuthenticatedMandatoryIossRequest(authDataRequest, testCredentials, vrn, iossNumber, regWrapperWithUnusableEmail, completeUserAnswers)
            val controller = new Harness

            val result = controller.callFilter(request).futureValue

            result.value mustEqual Redirect(controllers.routes.BusinessContactDetailsController.onPageLoad(changeRegWaypoint))

          }
        }

      }

      "and email has been updated" - {
        "must be None" in {

          val app = applicationBuilder(None)
            .build()

          val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
            registrationWrapper.registration.copy(schemeDetails =
              registrationWrapper.registration.schemeDetails.copy(unusableStatus = true)))

          val updatedUserAnswers = completeUserAnswers.set(BusinessContactDetailsPage, BusinessContactDetails(
            fullName = registrationWrapper.registration.schemeDetails.contactName,
            telephoneNumber = registrationWrapper.registration.schemeDetails.businessTelephoneNumber,
            emailAddress = s"1${registrationWrapper.registration.schemeDetails.businessEmailId}",
          )).success.value

          running(app) {
            val request = AuthenticatedMandatoryIossRequest(authDataRequest, testCredentials, vrn, iossNumber, regWrapperWithUnusableEmail, updatedUserAnswers)
            val controller = new Harness

            val result = controller.callFilter(request).futureValue

            result mustBe None

          }
        }
      }
    }

    "when the unusable email status is false" - {

      "must be None" in {

        val app = applicationBuilder(None)
          .build()

        running(app) {
          val request = AuthenticatedMandatoryIossRequest(authDataRequest, testCredentials, vrn, iossNumber, registrationWrapper, completeUserAnswers)
          val controller = new Harness

          val result = controller.callFilter(request).futureValue

          result mustBe None

        }
      }

    }

  }
}
