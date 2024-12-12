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

package connectors.test

import config.Service
import connectors.test.TestOnlyExternalResponseHttpParser.{ExternalResponseReads, ExternalResponseResponse}
import models.external.ExternalRequest
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyConnector @Inject()(
                                   config: Configuration,
                                   httpClientV2: HttpClientV2
                                 )(implicit ec: ExecutionContext) {

  private val baseUrl = config.get[Service](s"microservice.services.ioss-registration")

  def externalEntry(externalRequest: ExternalRequest, maybeLang: Option[String])(implicit hc: HeaderCarrier): Future[ExternalResponseResponse] = {
    val url: URL = maybeLang match {
      case Some(lang) => url"$baseUrl/external-entry?lang=$lang"
      case None => url"$baseUrl/external-entry"
    }
    httpClientV2.post(url).withBody(Json.toJson(externalRequest)).execute[ExternalResponseResponse]
  }
}