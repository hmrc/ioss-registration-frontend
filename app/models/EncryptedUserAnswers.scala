/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import models.domain.VatCustomerInfo
import play.api.libs.json.*
import queries.{Derivable, Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

case class EncryptedUserAnswers(
                              id: String,
                              data: String,
                              vatInfo: Option[VatCustomerInfo] = None,
                              lastUpdated: Instant = Instant.now
                            )

object EncryptedUserAnswers {

  val reads: Reads[EncryptedUserAnswers] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "_id").read[String] and
      (__ \ "data").read[String] and
      (__ \ "vatInfo").readNullable[VatCustomerInfo] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (EncryptedUserAnswers.apply _)
  }

  val writes: OWrites[EncryptedUserAnswers] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "_id").write[String] and
      (__ \ "data").write[String] and
      (__ \ "vatInfo").writeNullable[VatCustomerInfo] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (userAnswers => Tuple.fromProductTyped(userAnswers))
  }

  implicit val format: OFormat[EncryptedUserAnswers] = OFormat(reads, writes)
}
