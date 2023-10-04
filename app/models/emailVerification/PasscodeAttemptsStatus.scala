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

package models.emailVerification

import models.{Enumerable, WithName}


sealed trait PasscodeAttemptsStatus

object PasscodeAttemptsStatus extends Enumerable.Implicits {

  case object LockedPasscodeForSingleEmail extends WithName("lockedPasscodeForSingleEmail") with PasscodeAttemptsStatus
  case object LockedTooManyLockedEmails extends WithName("lockedTooManyLockedEmails") with PasscodeAttemptsStatus
  case object Verified extends WithName("verified") with PasscodeAttemptsStatus
  case object NotVerified extends WithName("notVerified") with PasscodeAttemptsStatus

  val values: Seq[PasscodeAttemptsStatus] =
    Seq(LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, Verified, NotVerified)

  implicit val enumerable: Enumerable[PasscodeAttemptsStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
