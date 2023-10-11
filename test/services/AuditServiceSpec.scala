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

package services

import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with BeforeAndAfterEach {
  private val auditConnector = mock[AuditConnector]
  private val mockAppConfig = mock[FrontendAppConfig]
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach() = {
    reset(auditConnector)
  }

  ".audit" - {

    /*TODO
    "must send Extended Event for create" in {
      when(auditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)

      val service = new AuditService(mockAppConfig, auditConnector)

      service.audit(RegistrationAuditModel(
        registrationAuditType = RegistrationAuditType.CreateRegistration,
        credId = "test",
        userAgent = "test",
        registration = registration,
        result = SubmissionResult.Success
      ))(hc, FakeRequest("POST", "test"))
      verify(auditConnector, times(1)).sendExtendedEvent(any())(any(), any())
    }

    "must send Extended Event for amend" in {
      when(auditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)

      val service = new AuditService(mockAppConfig, auditConnector)

      service.audit(RegistrationAuditModel(
        registrationAuditType = RegistrationAuditType.AmendRegistration,
        credId = "test",
        userAgent = "test",
        registration = registration,
        result = SubmissionResult.Success
      ))(hc, FakeRequest("POST", "test"))
      verify(auditConnector, times(1)).sendExtendedEvent(any())(any(), any())
    }*/
  }
}
