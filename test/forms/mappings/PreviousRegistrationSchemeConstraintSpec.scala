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

package forms.mappings

import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import models.domain.PreviousSchemeNumbers
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.validation.{Invalid, Valid}

class PreviousRegistrationSchemeConstraintSpec extends AnyFreeSpec with Matchers with PreviousRegistrationSchemeConstraint {

  private val country = Country.euCountries.head

  ".validatePreviousRegistrationScheme" - {

    "existing OSS schemes" - {

      "must return Valid when there's no existing OSS scheme" in {

        val existingSchemes: Seq[PreviousScheme] = Seq.empty
        val result = validatePreviousRegistrationSchemes(country.name, existingSchemes,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(0))(PreviousSchemeType.OSS)
        result mustEqual Valid
      }

      "must return Valid when there's one existing OSS scheme" in {

        val existingSchemes: Seq[PreviousScheme] = Seq(
          PreviousScheme.OSSU
        )
        val result = validatePreviousRegistrationSchemes(country.name, existingSchemes,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(1))(PreviousSchemeType.OSS)
        result mustEqual Valid
      }

      "must return Valid when there are two existing OSS schemes" in {

        val existingSchemes: Seq[PreviousScheme] = Seq(
          PreviousScheme.OSSU, PreviousScheme.OSSNU
        )
        val result = validatePreviousRegistrationSchemes(country.name, existingSchemes,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(2))(PreviousSchemeType.OSS)
        result mustEqual Invalid("previousScheme.oss.exceed.error", country.name)
      }
    }

    "editing existing OSS schemes" - {

      "must return Valid when there's one existing OSS scheme" in {

        val existingSchemes: Seq[PreviousScheme] = Seq(
          PreviousScheme.OSSU
        )
        val result = validatePreviousRegistrationSchemes(country.name, existingSchemes,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(0))(PreviousSchemeType.OSS)
        result mustEqual Valid
      }

      "must return Valid when there are two existing OSS schemes" in {

        val existingSchemes: Seq[PreviousScheme] = Seq(
          PreviousScheme.OSSU, PreviousScheme.OSSNU
        )
        val result = validatePreviousRegistrationSchemes(country.name, existingSchemes,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(1))(PreviousSchemeType.OSS)
        result mustEqual Valid
      }

    }

    "existing IOSS schemes" - {

      "must return Valid when there's one existing IOSS scheme" in {

        val previousIossScheme: Seq[PreviousScheme] = Seq.empty
        val result = validatePreviousRegistrationSchemes(country.name, previousIossScheme,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(0))(PreviousSchemeType.IOSS)
        result mustEqual Valid
      }

      "must return Invalid when there's two existing IOSS schemes" in {

        val previousIossScheme: Seq[PreviousScheme] = Seq(
          PreviousScheme.IOSSWI
        )
        val result = validatePreviousRegistrationSchemes(country.name, previousIossScheme,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(1))(PreviousSchemeType.IOSS)
        result mustEqual Invalid("previousScheme.ioss.exceed.error", country.name)
      }
    }

    "editing existing IOSS schemes" - {

      "must return Valid when there's one existing IOSS scheme" in {

        val previousIossScheme: Seq[PreviousScheme] = Seq(
          PreviousScheme.IOSSWI
        )
        val result = validatePreviousRegistrationSchemes(country.name, previousIossScheme,
          "previousScheme.oss.exceed.error", "previousScheme.ioss.exceed.error", Index(0))(PreviousSchemeType.IOSS)
        result mustEqual Valid
      }

    }
  }

  ".validatePreviousOssScheme" - {

    "must return Invalid if PreviousScheme.OSSNU already exists" in {

      val existingPreviousSchemes = Seq(PreviousScheme.OSSNU, PreviousScheme.IOSSWOI)

      val ossnuSchemeNumber = PreviousSchemeNumbers("EU23627834", None).previousSchemeNumber

      val result = validatePreviousOssScheme(country, existingPreviousSchemes, "previousScheme.oss.schemes.exceed.error")(ossnuSchemeNumber)

      result mustBe Invalid("previousScheme.oss.schemes.exceed.error", "non-union", country.name)
    }

    "must return Invalid if PreviousScheme.OSSU already exists" in {

      val existingPreviousScheme = Seq(PreviousScheme.OSSU, PreviousScheme.IOSSWI)

      val ossuSchemeNumber = PreviousSchemeNumbers(country.code + "123456781", None).previousSchemeNumber

      val result = validatePreviousOssScheme(country, existingPreviousScheme, "previousScheme.oss.schemes.exceed.error")(ossuSchemeNumber)

      result mustBe Invalid("previousScheme.oss.schemes.exceed.error", "union", country.name)
    }

    "must return Invalid if duplicate OSS previous schemes exist" - {

      "with union entered" in {

        val existingPreviousScheme = Seq(PreviousScheme.OSSU, PreviousScheme.OSSNU)

        val ossuSchemeNumber = PreviousSchemeNumbers(country.code + "234567812", None).previousSchemeNumber

        val result = validatePreviousOssScheme(country, existingPreviousScheme, "previousScheme.oss.schemes.exceed.error")(ossuSchemeNumber)

        result mustBe Invalid("previousScheme.oss.schemes.exceed.error", "union", country.name)
      }

      "with non-union entered" in {

        val existingPreviousScheme = Seq(PreviousScheme.OSSU, PreviousScheme.OSSNU)

        val ossuSchemeNumber = PreviousSchemeNumbers("EU234567812", None).previousSchemeNumber

        val result = validatePreviousOssScheme(country, existingPreviousScheme, "previousScheme.oss.schemes.exceed.error")(ossuSchemeNumber)

        result mustBe Invalid("previousScheme.oss.schemes.exceed.error", "non-union", country.name)
      }
    }

    "must return Valid if no duplicate OSS previous schemes exist" in {

      val existingPreviousScheme = Seq(PreviousScheme.OSSU, PreviousScheme.OSSNU)

      val iosswiSchemeNumbers = PreviousSchemeNumbers(country.code + "123456781", Some("123456782")).previousIntermediaryNumber.get

      val result = validatePreviousOssScheme(country, existingPreviousScheme, "previousScheme.oss.schemes.exceed.error")(iosswiSchemeNumbers)

      result mustBe Valid
    }

    "must return Valid if no previous schemes exist" in {

      val existingPreviousScheme = Seq.empty

      val result = validatePreviousOssScheme(country, existingPreviousScheme, "previousScheme.oss.schemes.exceed.error")("")

      result mustBe Valid
    }
  }

}
