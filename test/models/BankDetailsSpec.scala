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

package models

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class BankDetailsSpec extends AnyFreeSpec with Matchers with EitherValues {

  "BankDetails" - {

    "must exclude trailing and leading whitespace and double spaces" in {
      val ibanText = "GB33BUKB20201555555555"
      val bankDetails = Iban(ibanText).map { iban =>
        BankDetails(
          accountName = "      account    name        ",
          bic = Bic("ABCDEF2A"),
          iban = iban
        )
      }.value

      val json = Json.obj(
        "accountName" -> "account name",
        "bic" -> "ABCDEF2A",
        "iban" -> ibanText
      )

      json.as[BankDetails] mustEqual bankDetails
    }
  }
}
