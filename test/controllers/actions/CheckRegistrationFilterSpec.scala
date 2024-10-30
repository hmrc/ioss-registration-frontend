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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.oss.OssExclusionsService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CheckRegistrationFilterSpec extends SpecBase {

  private val iossEnrolmentKey = "HMRC-IOSS-ORG"
  private val ossEnrolmentKey = "HMRC-OSS-ORG"
  private val enrolment: Enrolment = Enrolment(iossEnrolmentKey, Seq.empty, "test", None)
  private val ossEnrolment: Enrolment = Enrolment(ossEnrolmentKey, Seq(EnrolmentIdentifier("VRN", vrn.vrn)), "test", None)

  private val mockOssExclusionsService: OssExclusionsService = mock[OssExclusionsService]

  class Harness(mode: RegistrationModificationMode, config: FrontendAppConfig, ossExclusionsService: OssExclusionsService)
    extends CheckRegistrationFilterImpl(mode, config, ossExclusionsService) {
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when an existing IOSS enrolment is not found" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(mode = NotModifyingExistingRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return None when an existing IOSS enrolment is found and is AmendingActiveRegistration mode" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None)
        val controller = new Harness(mode = AmendingActiveRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must redirect to Already Registered Controller when an existing IOSS enrolment is found" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None)
        val controller = new Harness(mode = NotModifyingExistingRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.routes.AlreadyRegisteredController.onPageLoad().url))
      }
    }

    "must redirect to Not Registered Controller when a registration is not found in AmendingActiveRegistration mode" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(mode = AmendingActiveRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url))
      }
    }

    "must redirect to Not Registered Controller when a registration is not found in RejoiningRegistration mode" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(mode = RejoiningRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url))
      }
    }

    "must redirect to Not Registered Controller when a registration is not found in Amend Previous Registration mode" in {

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None)
        val controller = new Harness(mode = AmendingPreviousRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url))
      }
    }

    // TODO -> Change Redirect and test name when new redirect implemented
    "must redirect to Scheme Quarantined Controller when an OSS registration is quarantined and Exclusion Reason:" +
      "Fails to Comply in NotModifyingExistingRegistration mode" in {

      when(mockOssExclusionsService.determineOssExclusionStatus(any())(any())) thenReturn true.toFuture

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(ossEnrolment)), None)
        val controller = new Harness(mode = NotModifyingExistingRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad().url))
      }
    }

    // TODO -> Change Redirect and test name when new redirect implemented
    "must redirect to Scheme Quarantined Controller when an OSS registration is quarantined and Exclusion Reason:" +
      "Fails to Comply in RejoiningRegistration mode" in {

      when(mockOssExclusionsService.determineOssExclusionStatus(any())(any())) thenReturn true.toFuture

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(ossEnrolment)), None)
        val controller = new Harness(mode = RejoiningRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(controllers.previousRegistrations.routes.SchemeQuarantinedController.onPageLoad().url))
      }
    }

    "must return None when an OSS registration is quarantined and Exclusion Reason:" +
      "Fails to Comply in AmendingActiveRegistration mode" in {

      when(mockOssExclusionsService.determineOssExclusionStatus(any())(any())) thenReturn true.toFuture

      val app = applicationBuilder(None)
        .overrides(bind[OssExclusionsService].toInstance(mockOssExclusionsService))
        .build()

      running(app) {

        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(ossEnrolment)), None)
        val controller = new Harness(mode = AmendingActiveRegistration, config, mockOssExclusionsService)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }
  }
}

