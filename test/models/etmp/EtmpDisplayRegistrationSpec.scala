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

package models.etmp

import base.SpecBase
import models.etmp.EtmpExclusionReason.{CeasedTrade, FailsToComply, NoLongerMeetsConditions, NoLongerSupplies, Reversal, TransferringMSID, VoluntarilyLeaves}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsSuccess, Json}
import testutils.RegistrationData.etmpDisplayRegistration

import java.time.LocalDate

class EtmpDisplayRegistrationSpec extends SpecBase with TableDrivenPropertyChecks {

  private val tradingNames: Seq[EtmpTradingName] = etmpDisplayRegistration.tradingNames
  private val schemeDetails: EtmpDisplaySchemeDetails = etmpDisplayRegistration.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpDisplayRegistration.bankDetails
  private val exclusions: Seq[EtmpExclusion] = etmpDisplayRegistration.exclusions
  private val adminUse: EtmpAdminUse = etmpDisplayRegistration.adminUse

  "EtmpDisplayRegistration" - {

    "must serialise/deserialise to and from EtmpDisplayRegistration" in {

      val displayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration

      val expectedJson = Json.obj(
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails,
        "exclusions" -> exclusions,
        "adminUse" -> adminUse
      )

      Json.toJson(displayRegistration) mustBe expectedJson
      expectedJson.validate[EtmpDisplayRegistration] mustBe JsSuccess(displayRegistration)
    }

    "canRejoinRegistration" - {
      val currentDate = LocalDate.now()

      val nonReversalEtmpExclusionReasons = Table(
        "etmpExclusionReason",
        NoLongerSupplies,
        CeasedTrade,
        NoLongerMeetsConditions,
        FailsToComply,
        VoluntarilyLeaves,
        TransferringMSID
      )

      "return true" - {
        "when it is not a reversal, not quarantined, not quarantined and the effective date is not in the future" in {
          forAll(nonReversalEtmpExclusionReasons) { nonReversalEtmpExclusionReason =>
            etmpDisplayRegistration.copy(exclusions = List(
                createExclusion(nonReversalEtmpExclusionReason, effectiveDate = currentDate))
              )
              .canRejoinRegistration(currentDate) mustBe true
          }
        }

        "when it is not a reversal, quarantined but the effectiveDate is more than 2 years ago" in {
          forAll(nonReversalEtmpExclusionReasons) { nonReversalEtmpExclusionReason =>
            etmpDisplayRegistration.copy(exclusions = List(
                createExclusion(nonReversalEtmpExclusionReason, effectiveDate = currentDate.minusYears(2).minusDays(1), quarantine = true))
              )
              .canRejoinRegistration(currentDate) mustBe true
          }
        }


      }

      def createExclusion(etmpExclusionReason: EtmpExclusionReason,
                          effectiveDate: LocalDate = LocalDate.now(),
                          quarantine: Boolean = false): EtmpExclusion = {
        EtmpExclusion(
          exclusionReason = etmpExclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(),
          quarantine = quarantine)
      }


      "return false" - {
        "for an empty exclusion list" in {
          etmpDisplayRegistration.copy(exclusions = List.empty).canRejoinRegistration(currentDate) mustBe false
        }

        // So how do we detect if a registration needs to reregister again?
        //if it's excluded, not a reversal, not quarantined (unless it's 2 years after the effective date), and not before the exclusion effective date (edited)
        "when the exclusion reason is a Reversal" in {
          etmpDisplayRegistration.copy(exclusions = List(createExclusion(Reversal)))
            .canRejoinRegistration(currentDate) mustBe false

        }


        "when the exclusion reason is not a Reversal and it is quarantined with an effective date is above or equal to 2 years ago" in {
          val dates = Table(
            "date",
            LocalDate.now().minusYears(2),
            LocalDate.now().minusYears(2).plusDays(1),
          )

          forAll(nonReversalEtmpExclusionReasons) { etmpExclusionReason =>
            forAll(dates) { date =>
              etmpDisplayRegistration.copy(exclusions = List(createExclusion(etmpExclusionReason, effectiveDate = date, quarantine = true)))
                .canRejoinRegistration(currentDate) mustBe false
            }
          }

        }
      }


    }
  }

}
