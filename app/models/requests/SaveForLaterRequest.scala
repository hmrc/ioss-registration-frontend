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

package models.requests

import models.domain.VatCustomerInfo
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.domain.Vrn


case class SaveForLaterRequest(
                                vrn: Vrn,
                                data: JsValue,
                                vatInfo: Option[VatCustomerInfo]
                              )

object SaveForLaterRequest {

  implicit val format: OFormat[SaveForLaterRequest] = Json.format[SaveForLaterRequest]

  def apply(data: JsValue, vrn: Vrn): SaveForLaterRequest = {
    // vatInfo is always pulled fresh
    // possibly worth having 2 models for clarity as vatInfo is not optional on retrieve
    // This is how it is modeled in the API
    SaveForLaterRequest(vrn = vrn, data = data, vatInfo = None)
  }
}
