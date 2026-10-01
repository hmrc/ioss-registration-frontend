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
import controllers.euDetails.routes as euRoutes
import controllers.routes
import models.UserAnswers
import models.domain.VatCustomerInfo
import models.euDetails.EuDetails
import models.requests.AuthenticatedDataRequest
import pages.{EmptyWaypoints, SavedProgressPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckPartOfVatGroupFilterSpec extends SpecBase {

  private val waypoints: Waypoints = EmptyWaypoints
  
  class Harness(restrictFromPartOfVatGroup: Boolean, registrationModificationMode: RegistrationModificationMode)
    extends CheckPartOfVatGroupFilterImpl(restrictFromPartOfVatGroup, registrationModificationMode) {
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
      compositeAccount = None
    )
  }

  ".filter" - {

    "must return None when restrictFromPartOfVatGroup is false" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest
        val controller = new Harness(restrictFromPartOfVatGroup = false, registrationModificationMode = NotModifyingExistingRegistration)

        val result = controller.callFilter(request).futureValue

        result `mustBe` None
      }
    }

    "must return None when part of VAT group is false and restrictFromPartOfVatGroup true" in {

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest
        val controller = new Harness(restrictFromPartOfVatGroup = true, registrationModificationMode = NotModifyingExistingRegistration)

        val result = controller.callFilter(request).futureValue

        result `mustBe` None
      }
    }

    "must redirect to Cannot Access Page when not in amend and part of VAT group true and restrictFromPartOfVatGroup true" in {

      val partOfVatGroupVatInfoAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(
        vatInfo = emptyUserAnswersWithVatInfo.vatInfo.map(_.copy(partOfVatGroup = true))
      )

      val application = applicationBuilder(None).build()

      running(application) {

        val request = dataRequest.copy(userAnswers = partOfVatGroupVatInfoAnswers)
        val controller = new Harness(restrictFromPartOfVatGroup = true, registrationModificationMode = NotModifyingExistingRegistration)

        val result = controller.callFilter(request).futureValue

        result `mustBe` Some(Redirect(routes.CannotAccessPageController.onPageLoad()))
      }
    }

    "when fixed establishments are present" - {

      "when in amend" - {

        "must redirect to Delete All Fixed Establishments As Part Of Vat Group Page when part of VAT group true" +
          " and restrictFromPartOfVatGroup true" in {

          val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value
          val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(AllEuDetailsQuery, List(euDetails)).success.value

          val partOfVatGroupVatInfoAnswers: UserAnswers = updatedAnswers.copy(
            vatInfo = emptyUserAnswersWithVatInfo.vatInfo.map(_.copy(partOfVatGroup = true))
          )

          val application = applicationBuilder(None).build()

          running(application) {

            val request = dataRequest.copy(userAnswers = partOfVatGroupVatInfoAnswers, registrationWrapper = Some(registrationWrapper))
            val controller = new Harness(restrictFromPartOfVatGroup = true, registrationModificationMode = AmendingActiveRegistration)

            val result = controller.callFilter(request).futureValue

            result `mustBe` Some(Redirect(euRoutes.DeleteAllFixedEstablishmentsAsPartOfVatGroupController.onPageLoad()))
          }
        }
      }

      "when not in amend or rejoin" - {

        "when saved answers are present" - {

          "must redirect to Saved Progress Remove Fixed Establishments Page when part of VAT group true" +
            " and restrictFromPartOfVatGroup false" in {

            val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value
            val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(AllEuDetailsQuery, List(euDetails)).success.value
              .set(SavedProgressPage, "/saved-redirect").success.value

            val partOfVatGroupVatInfoAnswers: UserAnswers = updatedAnswers.copy(
              vatInfo = emptyUserAnswersWithVatInfo.vatInfo.map(_.copy(partOfVatGroup = true))
            )

            val application = applicationBuilder(None).build()

            running(application) {

              val request = dataRequest.copy(userAnswers = partOfVatGroupVatInfoAnswers)
              val controller = new Harness(restrictFromPartOfVatGroup = false, registrationModificationMode = NotModifyingExistingRegistration)

              val result = controller.callFilter(request).futureValue

              result `mustBe` Some(Redirect(routes.SavedProgressRemoveFixedEstablishmentsController.onPageLoad(waypoints)))
            }
          }
        }
      }
    }

    "when fixed establishments are not present" - {

      "when not in amend or rejoin" - {

        "must None when part of VAT group false and restrictFromPartOfVatGroup false" in {

          val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value
          val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(AllEuDetailsQuery, List(euDetails)).success.value
            .set(SavedProgressPage, "/saved-redirect").success.value

          val partOfVatGroupVatInfoAnswers: UserAnswers = updatedAnswers.copy(
            vatInfo = emptyUserAnswersWithVatInfo.vatInfo.map(_.copy(partOfVatGroup = false))
          )

          val application = applicationBuilder(None).build()

          running(application) {

            val request = dataRequest.copy(userAnswers = partOfVatGroupVatInfoAnswers)
            val controller = new Harness(restrictFromPartOfVatGroup = false, registrationModificationMode = NotModifyingExistingRegistration)

            val result = controller.callFilter(request).futureValue

            result `mustBe` None
          }
        }
      }
    }
  }
}
