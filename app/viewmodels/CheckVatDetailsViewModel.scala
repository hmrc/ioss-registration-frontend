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

package viewmodels

import formats.Format.dateFormatter
import models.Country
import models.domain.VatCustomerInfo
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.domain.Vrn

case class CheckVatDetailsViewModel(vrn: Vrn, vatCustomerInfo: VatCustomerInfo)(implicit messages: Messages) {

  val organisationName: Option[String] = vatCustomerInfo.organisationName.map(organisationName => HtmlFormat.escape(organisationName).toString)

  val individualName: Option[String] = vatCustomerInfo.individualName.map(individualName => HtmlFormat.escape(individualName).toString)

  val formattedDate: String = vatCustomerInfo.registrationDate.format(dateFormatter)

  private val country: Option[Country] = Country.allCountries.find(_.code == vatCustomerInfo.desAddress.countryCode)

  val formattedAddress: Html = Html(
    Seq(
      Some(HtmlFormat.escape(vatCustomerInfo.desAddress.line1)),
      vatCustomerInfo.desAddress.line2.map(HtmlFormat.escape),
      vatCustomerInfo.desAddress.line3.map(HtmlFormat.escape),
      vatCustomerInfo.desAddress.line4.map(HtmlFormat.escape),
      vatCustomerInfo.desAddress.line5.map(HtmlFormat.escape),
      vatCustomerInfo.desAddress.postCode.map(HtmlFormat.escape),
      country.map(_.name)
    ).flatten.mkString("<br/>")
  )

  val partOfVatGroup: String =
    if (vatCustomerInfo.partOfVatGroup) {
      messages("site.yes")
    } else {
      messages("site.no")
    }
}
