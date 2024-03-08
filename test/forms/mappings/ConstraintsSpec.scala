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

import generators.Generators
import journey.JourneyHelpers
import models.Index
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.validation.{Invalid, Valid}

import java.time.LocalDate

class ConstraintsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with Constraints with JourneyHelpers {


  "firstError" - {

    "must return Valid when all constraints pass" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("foo")
      result mustEqual Valid
    }

    "must return Invalid when the first constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }

    "must return Invalid when the second constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.regexp", """^\w+$""")
    }

    "must return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength(-1, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result mustEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result mustEqual Invalid("error.max", 1)
    }
  }

  "inRange" - {

    "must return Invalid for a number less than the lower threshold" in {
      val result = inRange(1, 5, "error.max").apply(0)
      result mustEqual Invalid("error.max", 1, 5)
    }

    "must return Valid for a number equal to the lower threshold" in {
      val result = inRange(1, 5, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the upper threshold" in {
      val result = inRange(1, 5, "error.max").apply(5)
      result mustEqual Valid
    }

    "must return Valid for a number in range" in {
      val result = inRange(1, 5, "error.max").apply(3)
      result mustEqual Valid
    }

    "must return Invalid for a number above the upper threshold" in {
      val result = inRange(1, 5, "error.max").apply(6)
      result mustEqual Invalid("error.max", 1, 5)
    }
  }

  "regexp" - {

    "must return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result mustEqual Valid
    }

    "must return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result mustEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" - {

    "must return Valid for a string shorter than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 9)
      result mustEqual Valid
    }

    "must return Valid for an empty string" in {
      val result = maxLength(10, "error.length")("")
      result mustEqual Valid
    }

    "must return Valid for a string equal to the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 10)
      result mustEqual Valid
    }

    "must return Invalid for a string longer than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }
  }

  "maxDate" - {

    "must return Valid for a date before or equal to the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), max)
      } yield (max, date)

      forAll(gen) {
        case (max, date) =>

          val result = maxDate(max, "error.future")(date)
          result mustEqual Valid
      }
    }

    "must return Invalid for a date after the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(max.plusDays(1), LocalDate.of(3000, 1, 2))
      } yield (max, date)

      forAll(gen) {
        case (max, date) =>

          val result = maxDate(max, "error.future", "foo")(date)
          result mustEqual Invalid("error.future", "foo")
      }
    }
  }

  "minDate" - {

    "must return Valid for a date after or equal to the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(min, LocalDate.of(3000, 1, 1))
      } yield (min, date)

      forAll(gen) {
        case (min, date) =>

          val result = minDate(min, "error.past", "foo")(date)
          result mustEqual Valid
      }
    }

    "must return Invalid for a date before the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min <- datesBetween(LocalDate.of(2000, 1, 2), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), min.minusDays(1))
      } yield (min, date)

      forAll(gen) {
        case (min, date) =>

          val result = minDate(min, "error.past", "foo")(date)
          result mustEqual Invalid("error.past", "foo")
      }
    }
  }

  "nonEmptySet" - {

    "must return Valid when set is not empty" in {

      val set = Set("bar", "baz")

      val result = nonEmptySet("error.set")(set)
      result mustEqual Valid
    }

    "must return Invalid when set is empty" in {

      val set = Set.empty

      val result = nonEmptySet("error.set")(set)
      result mustEqual Invalid("error.set")
    }

  }

  "notADuplicate" - {

    "must return Valid when there is not another entry in the existing answers with the same value" in {

      val answer = "foo"
      val existingAnswers = Seq("bar", "baz")
      val index = Index(0)

      val result = notADuplicate(index, existingAnswers, "error.duplicate", "foo")(answer)
      result mustEqual Valid
    }

    "must return Valid when this answer is in the existing answers at the same index position, but nowhere else" in {

      val answer = "foo"
      val existingAnswers = Seq("bar", "foo", "baz")
      val index = Index(1)

      val result = notADuplicate(index, existingAnswers, "error.duplicate", "foo")(answer)
      result mustEqual Valid
    }

    "must return Invalid when this answer is in the existing answers at a different index position" in {

      val answer = "foo"
      val existingAnswers = Seq("bar", "foo", "baz")
      val index = Index(0)

      val result = notADuplicate(index, existingAnswers, "error.duplicate", "foo")(answer)
      result mustEqual Invalid("error.duplicate", "foo")
    }
  }

  "notContainStrings" - {

    "must return Valid when excludedStrings is empty" in {
      val answer = "name"
      val excludedStrings: Set[String] = Set()

      val result = notContainStrings(excludedStrings, "error.key")(answer)
      result mustEqual Valid
    }

    "must return Valid when answer does not include any excludedStrings" in {
      val answer = "name"
      val excludedStrings: Set[String] = Set("limited", "plc")

      val result = notContainStrings(excludedStrings, "error.key")(answer)
      result mustEqual Valid
    }

    "must return Valid when answer has excludedStrings as substring" in {
      val answer = "delimited"
      val excludedStrings: Set[String] = Set("limited", "plc")

      val result = notContainStrings(excludedStrings, "error.key")(answer)
      result mustEqual Valid
    }

    "must return Invalid when answer has excludedStrings as stand alone word" in {
      val answer = "name limited"
      val excludedStrings: Set[String] = Set("limited", "plc")

      val result = notContainStrings(excludedStrings, "error.key")(answer)
      result mustEqual Invalid("error.key")
    }
  }

  "validIossNumber" - {

    val traderPreviousRegistrations = Gen.listOfN(3, arbitraryPreviousRegistration.arbitrary).sample.value

    "must return Valid when the answer is a valid IOSS number registered to the trader" in {

      val answer = traderPreviousRegistrations.head.iossNumber

      val result = validIossNumber(traderPreviousRegistrations, "invalid.error.key")(answer)
      result mustBe Valid
    }

    "must return Invalid when the answer is not a valid IOSS number registered to the trader" in {

      val answer: String = arbitraryPreviousRegistration.arbitrary
        .suchThat(_.iossNumber.toSeq != traderPreviousRegistrations.map(_.iossNumber)).sample.value.iossNumber

      val result = validIossNumber(traderPreviousRegistrations, "invalid.error.key")(answer)
      result mustEqual Invalid("invalid.error.key")
    }
  }
}
