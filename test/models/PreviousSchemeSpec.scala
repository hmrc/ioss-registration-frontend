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

import models.etmp.SchemeType
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class PreviousSchemeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PreviousSchemePage" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PreviousScheme.values)

      forAll(gen) {
        previousSchemePage =>

          JsString(previousSchemePage.toString).validate[PreviousScheme].asOpt.value mustEqual previousSchemePage
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PreviousScheme.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PreviousScheme] mustEqual JsError("error.invalid")
      }
    }

    "must serialise valid values" in {
      val gen = Gen.oneOf(PreviousScheme.values)

      forAll(gen) { previousScheme =>
        Json.toJson(previousScheme) mustEqual JsString(previousScheme.toString)
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PreviousScheme.values)

      forAll(gen) {
        previousSchemePage =>

          Json.toJson(previousSchemePage) mustEqual JsString(previousSchemePage.toString)
      }
    }

    "fromEmtpSchemeType must map SchemeType to PreviousScheme correctly" in {
      PreviousScheme.fromEmtpSchemeType(SchemeType.OSSUnion) mustEqual PreviousScheme.OSSU
      PreviousScheme.fromEmtpSchemeType(SchemeType.OSSNonUnion) mustEqual PreviousScheme.OSSNU
      PreviousScheme.fromEmtpSchemeType(SchemeType.IOSSWithoutIntermediary) mustEqual PreviousScheme.IOSSWOI
      PreviousScheme.fromEmtpSchemeType(SchemeType.IOSSWithIntermediary) mustEqual PreviousScheme.IOSSWI
    }

    "toEmtpSchemaType must map PreviousScheme to SchemeType correctly" in {
      PreviousScheme.toEmtpSchemaType(PreviousScheme.OSSU) mustEqual SchemeType.OSSUnion
      PreviousScheme.toEmtpSchemaType(PreviousScheme.OSSNU) mustEqual SchemeType.OSSNonUnion
      PreviousScheme.toEmtpSchemaType(PreviousScheme.IOSSWOI) mustEqual SchemeType.IOSSWithoutIntermediary
      PreviousScheme.toEmtpSchemaType(PreviousScheme.IOSSWI) mustEqual SchemeType.IOSSWithIntermediary
    }

    "values must contain all PreviousScheme instances" in {
      PreviousScheme.values must contain allElementsOf Seq(
        PreviousScheme.OSSU,
        PreviousScheme.OSSNU,
        PreviousScheme.IOSSWOI,
        PreviousScheme.IOSSWI
      )
    }

    "iossValues must contain only IOSS-related PreviousScheme instances" in {
      PreviousScheme.iossValues must contain allElementsOf Seq(
        PreviousScheme.IOSSWOI,
        PreviousScheme.IOSSWI
      )
    }

    "options must generate the correct RadioItems" in {
      implicit val messages: Messages = mock[Messages]
      when(messages.apply(any[String], any())).thenReturn("mocked_message")

      val options = PreviousScheme.options

      options.zipWithIndex.foreach {
        case (radioItem, index) =>
          radioItem.content mustEqual Text("mocked_message")
          radioItem.value mustBe Some(PreviousScheme.values(index).toString)
          radioItem.id mustBe Some(s"value_$index")
      }
    }
  }
}
