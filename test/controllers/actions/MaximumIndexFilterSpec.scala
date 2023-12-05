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

package controllers.actions

import base.SpecBase
import models.Index
import models.requests.AuthenticatedDataRequest
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MaximumIndexFilterSpec extends SpecBase with ScalaCheckPropertyChecks {

  class Harness(index: Index, max: Int) extends MaximumIndexFilter(index, max) {
    def callFilter(request: AuthenticatedDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return NotFound when the current index's position is greater than or equal to the maximum allowed" in {

      val invalidCombinations = for {
        index <- arbitrary[Int] suchThat (_ > Int.MinValue) suchThat (_ < Int.MaxValue) map (Index(_))
        max <- Gen.choose(Int.MinValue, index.position + 1)
      } yield (index, max)

      forAll(invalidCombinations) {
        case (index, max) =>
          val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers, None)
          val harness = new Harness(index, max)

          val result = harness.callFilter(request).futureValue

          result.value mustBe NotFound
      }
    }

    "must return None when the current index's position is less than the maximum allowed" in {

      val validCombinations = for {
        index <- arbitrary[Int] suchThat (_ < Int.MaxValue - 1) map (Index(_))
        max <- Gen.choose(index.position + 1, Int.MaxValue)
      } yield (index, max)

      forAll(validCombinations) {
        case (index, max) =>
          val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers, None)
          val harness = new Harness(index, max)

          val result = harness.callFilter(request).futureValue

          result must not be defined
      }
    }
  }
}
