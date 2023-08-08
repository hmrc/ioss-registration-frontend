package connectors

import base.SpecBase
import play.api.Application
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    applicationBuilder()
      .build()

  ".getCustomerVatInfo" - {

    val url: String = s"/vat-information"

    "must return vat information when the backend returns some" in {

      running(application) {
        val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

        val vatInfo: VatCustomerInfo = vatCustomerInfo
      }
    }
  }

}
