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

package models.core

import base.SpecBase
import models.core.MatchType._

class MatchTypeSpec extends SpecBase {

  "MatchType" - {
    "isActiveTrader" - {
      "must return true for active match types" in {
        val activeTypes = Seq(FixedEstablishmentActiveNETP, TraderIdActiveNETP, OtherMSNETPActiveNETP)

        for (activeType <- activeTypes) {
          activeType.isActiveTrader mustBe true
        }
      }

      "must return false for not active match types" in {
        val activeTypes = Seq(TraderIdQuarantinedNETP, OtherMSNETPQuarantinedNETP, FixedEstablishmentQuarantinedNETP, TransferringMSID, PreviousRegistrationFound)

        for (activeType <- activeTypes) {
          activeType.isActiveTrader mustBe false
        }
      }
    }

    "isQuarantinedTrader" - {

      "must return true for quarantined match types" in {
        val activeTypes = Seq(TraderIdQuarantinedNETP, OtherMSNETPQuarantinedNETP, FixedEstablishmentQuarantinedNETP)

        for (activeType <- activeTypes) {
          activeType.isQuarantinedTrader mustBe true
        }
      }

      "must return false for non quarantined match types" in {
        val activeTypes = Seq(FixedEstablishmentActiveNETP, TraderIdActiveNETP, OtherMSNETPActiveNETP, TransferringMSID, PreviousRegistrationFound)

        for (activeType <- activeTypes) {
          activeType.isQuarantinedTrader mustBe false
        }
      }
    }
  }

}
