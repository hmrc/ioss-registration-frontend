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
