package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class ViewOrChangePreviousRegistrationsMuiltipleSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "ViewOrChangePreviousRegistrationsMultiple" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(ViewOrChangePreviousRegistrationsMuiltiple.values.toSeq)

      forAll(gen) {
        viewOrChangePreviousRegistrationsMuiltiple =>

          JsString(viewOrChangePreviousRegistrationsMuiltiple.toString).validate[ViewOrChangePreviousRegistrationsMuiltiple].asOpt.value mustEqual viewOrChangePreviousRegistrationsMuiltiple
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!ViewOrChangePreviousRegistrationsMuiltiple.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[ViewOrChangePreviousRegistrationsMuiltiple] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(ViewOrChangePreviousRegistrationsMuiltiple.values.toSeq)

      forAll(gen) {
        viewOrChangePreviousRegistrationsMuiltiple =>

          Json.toJson(viewOrChangePreviousRegistrationsMuiltiple) mustEqual JsString(viewOrChangePreviousRegistrationsMuiltiple.toString)
      }
    }
  }
}
