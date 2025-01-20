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

package models.amend

import base.SpecBase
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.LocalDate

class PreviousRegistrationSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val previousRegistrations = Gen.listOfN(3, arbitraryPreviousRegistration.arbitrary).sample.value

  "PreviousRegistration" - {

    "must serialise/deserialise to and from PreviousRegistration" in {

      val previousRegistration: PreviousRegistration = previousRegistrations.head

      val expectedJson = Json.obj(
        "iossNumber" -> previousRegistration.iossNumber,
        "startPeriod" -> previousRegistration.startPeriod,
        "endPeriod" -> previousRegistration.endPeriod
      )

      Json.toJson(previousRegistration) mustBe expectedJson
      expectedJson.validate[PreviousRegistration] mustBe JsSuccess(previousRegistration)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[PreviousRegistration] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val previousRegistration: PreviousRegistration = previousRegistrations.head

      val expectedJson = Json.obj(
        "iossNumber" -> 12345,
        "startPeriod" -> previousRegistration.startPeriod,
        "endPeriod" -> previousRegistration.endPeriod
      )

      expectedJson.validate[PreviousRegistration] mustBe a[JsError]

    }

    "must populate Radio Items correctly" in {

      val application = applicationBuilder().build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val previousRegistrations: Seq[PreviousRegistration] = Seq(
          PreviousRegistration(
            iossNumber = "IM900123456789",
            startPeriod = LocalDate.of(2023, 12, 1),
            endPeriod = LocalDate.of(2024, 1, 3)
          ),
          PreviousRegistration(
            iossNumber = "IM900987654321",
            startPeriod = LocalDate.of(2024, 2, 1),
            endPeriod = LocalDate.of(2024, 3, 3)
          ),
          PreviousRegistration(
            iossNumber = "IM900123456321",
            startPeriod = LocalDate.of(2024, 3, 1),
            endPeriod = LocalDate.of(2024, 4, 30)
          )
        )

        val expectedResult = Seq(
          RadioItem(
            content = Text("December 2023 to January 2024"),
            id = Some(s"value_0"),
            value = Some("IM900123456789"),
            hint = Some(Hint(content = HtmlContent("IOSS number: IM900123456789")))
          ),
          RadioItem(
            content = Text("February 2024 to March 2024"),
            id = Some(s"value_1"),
            value = Some("IM900987654321"),
            hint = Some(Hint(content = HtmlContent("IOSS number: IM900987654321")))
          ),
          RadioItem(
            content = Text("March 2024 to April 2024"),
            id = Some(s"value_2"),
            value = Some("IM900123456321"),
            hint = Some(Hint(content = HtmlContent("IOSS number: IM900123456321")))
          )
        )

        val result = PreviousRegistration.options(previousRegistrations)

        result mustBe expectedResult
      }
    }
  }
}
