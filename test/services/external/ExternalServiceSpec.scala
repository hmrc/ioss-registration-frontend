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

package services.external

import base.SpecBase
import models.SessionData
import models.external.{ExternalRequest, ExternalResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExternalServiceSpec extends SpecBase {

  "getExternalResponse" - {
    "must return ExternalResponse and save url in session" in {
      val externalRequest = ExternalRequest("BTA", "/business-account")
      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.get(any())) thenReturn Future.successful(Seq.empty)
      when(sessionRepository.set(any())) thenReturn Future.successful(true)
      val service = new ExternalService(sessionRepository)
      service.getExternalResponse(externalRequest, "id").futureValue mustBe ExternalResponse(controllers.routes.IndexController.onPageLoad().url)
      verify(sessionRepository, times(1)).set(any())
    }
  }

}