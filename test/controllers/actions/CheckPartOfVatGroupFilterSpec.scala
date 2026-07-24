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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.UserAnswers
import models.amend.RegistrationWrapper
import models.requests.AuthenticatedDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckPartOfVatGroupFilterSpec extends SpecBase {

  class Harness(restrictFromPartOfVatGroup: Boolean) extends CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup) {
    def callFilter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = filter(request)
  }

  private val ossEnrolmentKey = "HMRC-OSS-ORG"
  private val ossEnrolment: Enrolment = Enrolment(ossEnrolmentKey, Seq(EnrolmentIdentifier("VRN", vrn.vrn)), "test", None)

  private val dataRequest: AuthenticatedDataRequest[AnyContentAsEmpty.type] = {
    AuthenticatedDataRequest(
      request = FakeRequest(),
      credentials = testCredentials,
      vrn = vrn,
      enrolments = Enrolments(Set(ossEnrolment)),
      iossNumber = None,
      userAnswers = emptyUserAnswersWithVatInfo,
      registrationWrapper = None,
      numberOfIossRegistrations = 0,
      latestOssRegistration = None
    )
  }

  ".filter" - {

    "must return None when restrictFromPartOfVatGroup is false" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest
        val controller = new Harness(restrictFromPartOfVatGroup = false)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return None when part of VAT group is false and restrictFromPartOfVatGroup true" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest
        val controller = new Harness(restrictFromPartOfVatGroup = true)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must redirect to Cannot Access Page when part of VAT group true and restrictFromPartOfVatGroup true" in {

      val partOfVatGroupVatInfoAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(
        vatInfo = emptyUserAnswersWithVatInfo.vatInfo.map(_.copy(partOfVatGroup = true))
      )

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest.copy(userAnswers = partOfVatGroupVatInfoAnswers)
        val controller = new Harness(restrictFromPartOfVatGroup = true)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(routes.CannotAccessPageController.onPageLoad()))
      }
    }

    "must throw an IllegalStateException when restrictFromPartOfVatGroup true but there is no VAT information available" in {

      val errorMessage = "VAT info unavailable, must have VAT info."

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest.copy(userAnswers = emptyUserAnswers)
        val controller = new Harness(restrictFromPartOfVatGroup = true)

        intercept[IllegalStateException] {
          controller.callFilter(request).futureValue
        }.getMessage mustBe errorMessage
      }
    }
  }
}
