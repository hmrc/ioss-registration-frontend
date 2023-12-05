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

package models

import logging.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import viewmodels.govuk.select._

case class Country(code: String, name: String)

case class CountryWithValidationDetails(country: Country, vrnRegex: String, messageInput: String, exampleVrn: String)

object Country {

  implicit val format: OFormat[Country] = Json.format[Country]

  def fromCountryCode(countryCode: String): Option[Country] = {
    euCountries.find(_.code == countryCode)
  }

  def fromCountryCodeUnsafe(countryCode: String): Country = {
    fromCountryCode(countryCode)
      .getOrElse(throw new RuntimeException(s"countryCode $countryCode could not be mapped to a country"))
  }

  val euCountries: Seq[Country] = Seq(
    Country("AT", "Austria"),
    Country("BE", "Belgium"),
    Country("BG", "Bulgaria"),
    Country("HR", "Croatia"),
    Country("CY", "Republic of Cyprus"),
    Country("CZ", "Czech Republic"),
    Country("DK", "Denmark"),
    Country("EE", "Estonia"),
    Country("FI", "Finland"),
    Country("FR", "France"),
    Country("DE", "Germany"),
    Country("EL", "Greece"),
    Country("HU", "Hungary"),
    Country("IE", "Ireland"),
    Country("IT", "Italy"),
    Country("LV", "Latvia"),
    Country("LT", "Lithuania"),
    Country("LU", "Luxembourg"),
    Country("MT", "Malta"),
    Country("NL", "Netherlands"),
    Country("PL", "Poland"),
    Country("PT", "Portugal"),
    Country("RO", "Romania"),
    Country("SK", "Slovakia"),
    Country("SI", "Slovenia"),
    Country("ES", "Spain"),
    Country("SE", "Sweden")
  )

  val euCountrySelectItems: Seq[SelectItem] = {
    SelectItem(value = None, text = "Select a country") +:
      euCountries.map {
        country =>
          SelectItemViewModel(
            value = country.code,
            text = country.name
          )
      }
  }

  val allCountries: Seq[Country] = Seq(
    Country("AF", "Afghanistan"),
    Country("AL", "Albania"),
    Country("DZ", "Algeria"),
    Country("AD", "Andorra"),
    Country("AO", "Angola"),
    Country("AG", "Antigua and Barbuda"),
    Country("AR", "Argentina"),
    Country("AM", "Armenia"),
    Country("AU", "Australia"),
    Country("AT", "Austria"),
    Country("AZ", "Azerbaijan"),
    Country("BH", "Bahrain"),
    Country("BD", "Bangladesh"),
    Country("BB", "Barbados"),
    Country("BY", "Belarus"),
    Country("BE", "Belgium"),
    Country("BZ", "Belize"),
    Country("BJ", "Benin"),
    Country("BT", "Bhutan"),
    Country("BO", "Bolivia"),
    Country("BA", "Bosnia and Herzegovina"),
    Country("BW", "Botswana"),
    Country("BR", "Brazil"),
    Country("BN", "Brunei"),
    Country("BG", "Bulgaria"),
    Country("BF", "Burkina Faso"),
    Country("BI", "Burundi"),
    Country("KH", "Cambodia"),
    Country("CM", "Cameroon"),
    Country("CA", "Canada"),
    Country("CV", "Cape Verde"),
    Country("CF", "Central African Republic"),
    Country("TD", "Chad"),
    Country("CL", "Chile"),
    Country("CN", "China"),
    Country("CO", "Colombia"),
    Country("KM", "Comoros"),
    Country("CG", "Congo"),
    Country("CD", "Congo (Democratic Republic)"),
    Country("CR", "Costa Rica"),
    Country("HR", "Croatia"),
    Country("CU", "Cuba"),
    Country("CY", "Cyprus"),
    Country("CZ", "Czechia"),
    Country("DK", "Denmark"),
    Country("DJ", "Djibouti"),
    Country("DM", "Dominica"),
    Country("DO", "Dominican Republic"),
    Country("TL", "East Timor"),
    Country("EC", "Ecuador"),
    Country("EG", "Egypt"),
    Country("SV", "El Salvador"),
    Country("GQ", "Equatorial Guinea"),
    Country("ER", "Eritrea"),
    Country("EE", "Estonia"),
    Country("SZ", "Eswatini"),
    Country("ET", "Ethiopia"),
    Country("FJ", "Fiji"),
    Country("FI", "Finland"),
    Country("FR", "France"),
    Country("GA", "Gabon"),
    Country("GE", "Georgia"),
    Country("DE", "Germany"),
    Country("GH", "Ghana"),
    Country("EL", "Greece"),
    Country("GD", "Grenada"),
    Country("GT", "Guatemala"),
    Country("GN", "Guinea"),
    Country("GW", "Guinea-Bissau"),
    Country("GY", "Guyana"),
    Country("HT", "Haiti"),
    Country("HN", "Honduras"),
    Country("HU", "Hungary"),
    Country("IS", "Iceland"),
    Country("IN", "India"),
    Country("ID", "Indonesia"),
    Country("IR", "Iran"),
    Country("IQ", "Iraq"),
    Country("IE", "Ireland"),
    Country("IL", "Israel"),
    Country("IT", "Italy"),
    Country("CI", "Ivory Coast"),
    Country("JM", "Jamaica"),
    Country("JP", "Japan"),
    Country("JO", "Jordan"),
    Country("KZ", "Kazakhstan"),
    Country("KE", "Kenya"),
    Country("KI", "Kiribati"),
    Country("XK", "Kosovo"),
    Country("KW", "Kuwait"),
    Country("KG", "Kyrgyzstan"),
    Country("LA", "Laos"),
    Country("LV", "Latvia"),
    Country("LB", "Lebanon"),
    Country("LS", "Lesotho"),
    Country("LR", "Liberia"),
    Country("LY", "Libya"),
    Country("LI", "Liechtenstein"),
    Country("LT", "Lithuania"),
    Country("LU", "Luxembourg"),
    Country("MG", "Madagascar"),
    Country("MW", "Malawi"),
    Country("MY", "Malaysia"),
    Country("MV", "Maldives"),
    Country("ML", "Mali"),
    Country("MT", "Malta"),
    Country("MH", "Marshall Islands"),
    Country("MR", "Mauritania"),
    Country("MU", "Mauritius"),
    Country("MX", "Mexico"),
    Country("FM", "Micronesia"),
    Country("MD", "Moldova"),
    Country("MC", "Monaco"),
    Country("MN", "Mongolia"),
    Country("ME", "Montenegro"),
    Country("MA", "Morocco"),
    Country("MZ", "Mozambique"),
    Country("MM", "Myanmar (Burma)"),
    Country("NA", "Namibia"),
    Country("NR", "Nauru"),
    Country("NP", "Nepal"),
    Country("NL", "Netherlands"),
    Country("NZ", "New Zealand"),
    Country("NI", "Nicaragua"),
    Country("NE", "Niger"),
    Country("NG", "Nigeria"),
    Country("KP", "North Korea"),
    Country("MK", "North Macedonia"),
    Country("NO", "Norway"),
    Country("OM", "Oman"),
    Country("PK", "Pakistan"),
    Country("PW", "Palau"),
    Country("PA", "Panama"),
    Country("PG", "Papua New Guinea"),
    Country("PY", "Paraguay"),
    Country("PE", "Peru"),
    Country("PH", "Philippines"),
    Country("PL", "Poland"),
    Country("PT", "Portugal"),
    Country("QA", "Qatar"),
    Country("RO", "Romania"),
    Country("RU", "Russia"),
    Country("RW", "Rwanda"),
    Country("WS", "Samoa"),
    Country("SM", "San Marino"),
    Country("ST", "Sao Tome and Principe"),
    Country("SA", "Saudi Arabia"),
    Country("SN", "Senegal"),
    Country("RS", "Serbia"),
    Country("SC", "Seychelles"),
    Country("SL", "Sierra Leone"),
    Country("SG", "Singapore"),
    Country("SK", "Slovakia"),
    Country("SI", "Slovenia"),
    Country("SB", "Solomon Islands"),
    Country("SO", "Somalia"),
    Country("ZA", "South Africa"),
    Country("KR", "South Korea"),
    Country("SS", "South Sudan"),
    Country("ES", "Spain"),
    Country("LK", "Sri Lanka"),
    Country("KN", "St Kitts and Nevis"),
    Country("LC", "St Lucia"),
    Country("VC", "St Vincent"),
    Country("SD", "Sudan"),
    Country("SR", "Suriname"),
    Country("SE", "Sweden"),
    Country("CH", "Switzerland"),
    Country("SY", "Syria"),
    Country("TJ", "Tajikistan"),
    Country("TZ", "Tanzania"),
    Country("TH", "Thailand"),
    Country("BS", "The Bahamas"),
    Country("GM", "The Gambia"),
    Country("TG", "Togo"),
    Country("TO", "Tonga"),
    Country("TT", "Trinidad and Tobago"),
    Country("TN", "Tunisia"),
    Country("TR", "Turkey"),
    Country("TM", "Turkmenistan"),
    Country("TV", "Tuvalu"),
    Country("UG", "Uganda"),
    Country("UA", "Ukraine"),
    Country("AE", "United Arab Emirates"),
    Country("GB", "United Kingdom"),
    Country("US", "United States"),
    Country("UY", "Uruguay"),
    Country("UZ", "Uzbekistan"),
    Country("VU", "Vanuatu"),
    Country("VA", "Vatican City"),
    Country("VE", "Venezuela"),
    Country("VN", "Vietnam"),
    Country("YE", "Yemen"),
    Country("ZM", "Zambia"),
    Country("ZW", "Zimbabwe")
  )

  val internationalCountries: Seq[Country] =
    allCountries.filterNot(_.code == "GB")

  def getCountryName(countryCode: String): String = euCountries.filter(_.code == countryCode).map(_.name).head
}

object CountryWithValidationDetails extends Logging {

  lazy val euCountriesWithVRNValidationRules: Seq[CountryWithValidationDetails] = Seq(
    CountryWithValidationDetails(Country("AT", "Austria"), austriaVatNumberRegex, "the 9 characters", "U12345678"),
    CountryWithValidationDetails(Country("BE", "Belgium"), belgiumVatNumberRegex, "the 10 numbers", "0123456789"),
    CountryWithValidationDetails(Country("BG", "Bulgaria"), bulgariaVatNumberRegex, "9 or 10 numbers", "123456789"),
    CountryWithValidationDetails(Country("HR", "Croatia"), croatiaVatNumberRegex, "the 11 numbers", "01234567899"),
    CountryWithValidationDetails(Country("CY", "Republic of Cyprus"), cyprusVatNumberRegex, "the 9 characters", "12345678X"),
    CountryWithValidationDetails(Country("CZ", "Czech Republic"), czechRepublicVatNumberRegex, "8, 9 or 10 numbers", "123456789"),
    CountryWithValidationDetails(Country("DK", "Denmark"), denmarkVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("EE", "Estonia"), estoniaVatNumberRegex, "the 9 numbers", "123456789"),
    CountryWithValidationDetails(Country("FI", "Finland"), finlandVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("FR", "France"), franceVatNumberRegex, "the 11 characters", "XX123456789"),
    CountryWithValidationDetails(Country("DE", "Germany"), germanyVatNumberRegex, "the 9 numbers", "123456789"),
    CountryWithValidationDetails(Country("EL", "Greece"), greeceVatNumberRegex, "the 9 numbers", "123456789"),
    CountryWithValidationDetails(Country("HU", "Hungary"), hungaryVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("IE", "Ireland"), irelandVatNumberRegex, "8 or 9 characters", "1234567XX"),
    CountryWithValidationDetails(Country("IT", "Italy"), italyVatNumberRegex, "the 11 numbers", "01234567899"),
    CountryWithValidationDetails(Country("LV", "Latvia"), latviaVatNumberRegex, "the 11 numbers", "01234567899"),
    CountryWithValidationDetails(Country("LT", "Lithuania"), lithuaniaVatNumberRegex, "9 or 12 numbers", "123456789"),
    CountryWithValidationDetails(Country("LU", "Luxembourg"), luxembourgVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("MT", "Malta"), maltaVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("NL", "Netherlands"), netherlandsVatNumberRegex, "the 12 characters", "0123456789AB"),
    CountryWithValidationDetails(Country("PL", "Poland"), polandVatNumberRegex, "the 10 numbers", "1234567890"),
    CountryWithValidationDetails(Country("PT", "Portugal"), portugalVatNumberRegex, "the 9 numbers", "123456789"),
    CountryWithValidationDetails(Country("RO", "Romania"), romaniaVatNumberRegex, "between 2 and 10 numbers", "1234567890"),
    CountryWithValidationDetails(Country("SK", "Slovakia"), slovakiaVatNumberRegex, "the 10 numbers", "1234567890"),
    CountryWithValidationDetails(Country("SI", "Slovenia"), sloveniaVatNumberRegex, "the 8 numbers", "12345678"),
    CountryWithValidationDetails(Country("ES", "Spain"), spainVatNumberRegex, "the 9 characters", "X1234567X"),
    CountryWithValidationDetails(Country("SE", "Sweden"), swedenVatNumberRegex, "the 12 numbers", "012345678987")
  )

  private val austriaVatNumberRegex = """^ATU[0-9]{8}$"""
  private val belgiumVatNumberRegex = """^BE(0|1)[0-9]{9}$"""
  private val bulgariaVatNumberRegex = """^BG[0-9]{9,10}$"""
  private val cyprusVatNumberRegex = """^CY[0-9]{8}[A-Z]$"""
  private val czechRepublicVatNumberRegex = """^CZ[0-9]{8,10}$"""
  private val germanyVatNumberRegex = """^DE[0-9]{9}$"""
  private val denmarkVatNumberRegex = """^DK[0-9]{8}$"""
  private val estoniaVatNumberRegex = """^EE[0-9]{9}$"""
  private val greeceVatNumberRegex = """^EL[0-9]{9}$"""
  private val spainVatNumberRegex = """^ES[A-Z][0-9]{8}$|^ES[0-9]{8}[A-Z]$|^ES[A-Z][0-9]{7}[A-Z]$"""
  private val finlandVatNumberRegex = """^FI[0-9]{8}$"""
  private val franceVatNumberRegex = """^FR[A-Z0-9]{2}[0-9]{9}$"""
  private val croatiaVatNumberRegex = """^HR[0-9]{11}$"""
  private val hungaryVatNumberRegex = """^HU[0-9]{8}$"""
  private val irelandVatNumberRegex = """^IE([0-9][A-Z][0-9]{5}[A-Z]|[0-9]{7}[A-Z0-9]{1,2})$"""
  private val italyVatNumberRegex = """^IT[0-9]{11}$"""
  private val lithuaniaVatNumberRegex = """^LT[0-9]{9}$|^LT[0-9]{12}$"""
  private val luxembourgVatNumberRegex = """^LU[0-9]{8}$"""
  private val latviaVatNumberRegex = """^LV[0-9]{11}$"""
  private val maltaVatNumberRegex = """^MT[0-9]{8}$"""
  private val netherlandsVatNumberRegex = """^NL[A-Z0-9\+\*]{12}$"""
  private val polandVatNumberRegex = """^PL[0-9]{10}$"""
  private val portugalVatNumberRegex = """^PT[0-9]{9}$"""
  private val romaniaVatNumberRegex = """^RO[0-9]{2,10}$"""
  private val swedenVatNumberRegex = """^SE[0-9]{12}$"""
  private val sloveniaVatNumberRegex = """^SI[0-9]{8}$"""
  private val slovakiaVatNumberRegex = """^SK[0-9]{10}$"""


  def convertTaxIdentifierForTransfer(identifier: String, countryCode: String): String = {

    CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode) match {
      case Some(countryValidationDetails) =>
        if (identifier.matches(countryValidationDetails.vrnRegex)) {
          identifier.substring(2)
        } else if (identifier.substring(2).matches(countryValidationDetails.vrnRegex)) {
          identifier.substring(4)
        } else {
          identifier
        }

      case _ =>
        logger.error("Error occurred while getting country code regex, unable to convert identifier")
        throw new IllegalStateException("Error occurred while getting country code regex, unable to convert identifier")
    }
  }
}
