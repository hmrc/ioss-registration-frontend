package models

import base.SpecBase
import models.etmp.{EtmpBankDetails, EtmpTradingName}
import play.api.libs.json.{JsError, JsSuccess, Json}
import testutils.GenerateCompositeAccount.generateCompositeAccount

class CompositeAccountSpec extends SpecBase {

  private val compositeAccount: Option[CompositeAccount] = generateCompositeAccount(ossRegistration)

  "CompositeAccount" - {

    "must serialise / deserialise from and to a CompositeAccount" in {

      val json = Json.obj(
        "tradingNames" -> compositeAccount.value.tradingNames,
        "contactDetails" -> compositeAccount.value.contactDetails,
        "bankDetails" -> compositeAccount.value.bankDetails
      )

      val expectedResult = CompositeAccount(
        tradingNames = compositeAccount.value.tradingNames,
        contactDetails = compositeAccount.value.contactDetails,
        bankDetails = compositeAccount.value.bankDetails
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[CompositeAccount] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[CompositeAccount] `mustBe` a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "tradingNames" -> compositeAccount.value.tradingNames,
        "contactDetails" -> 12345,
        "bankDetails" -> compositeAccount.value.bankDetails
      )

      json.validate[CompositeAccount] `mustBe` a[JsError]
    }

    ".fromEtmpTradingName" - {

      "must successfully convert a Seq[EtmpTradingName] to Seq[TradingName]" in {

        val etmpTradingNames: Seq[EtmpTradingName] = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.tradingNames

        val result = CompositeAccount.fromEtmpTradingName(etmpTradingNames)

        val expectedResult: Seq[TradingName] = etmpTradingNames.map { tn =>
          TradingName(tn.tradingName)
        }

        result `mustBe` expectedResult
      }
    }

    ".fromEtmpBankDetails" - {

      "must successfully convert EtmpBankDetails to BankDetails" in {

        val etmpBankDetails: EtmpBankDetails = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.bankDetails

        val result = CompositeAccount.fromEtmpBankDetails(etmpBankDetails)

        val expectedResult: BankDetails = BankDetails(
          accountName = etmpBankDetails.accountName,
          bic = etmpBankDetails.bic,
          iban = etmpBankDetails.iban
        )

        result `mustBe` expectedResult
      }
    }
  }
}
