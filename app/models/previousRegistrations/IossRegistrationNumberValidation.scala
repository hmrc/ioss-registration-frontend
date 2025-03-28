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

package models.previousRegistrations

import models.Country


case class IossRegistrationNumberValidation(country: Country, vrnRegex: String, messageInput: String)

object IossRegistrationNumberValidation {

  lazy val euCountriesWithIOSSValidationRules: Seq[IossRegistrationNumberValidation] = Seq(
    IossRegistrationNumberValidation(Country("AT", "Austria"),austriaIossNumberRegex, "This will start with IM040 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("BE", "Belgium"), belgiumIossNumberRegex, "This will start with IM056 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("BG", "Bulgaria"), bulgariaIossNumberRegex, "This will start with IM100 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("HR", "Croatia"), croatiaIossNumberRegex, "This will start with IM191 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("CY", "Cyprus"), cyprusIossNumberRegex, "This will start with IM196 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("CZ", "Czech Republic"), czechRepublicIossNumberRegex, "This will start with IM203 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("DK", "Denmark"), denmarkIossNumberRegex, "This will start with IM208 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("EE", "Estonia"), estoniaIossNumberRegex, "This will start with IM233 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("FI", "Finland"), finlandIossNumberRegex, "This will start with IM246 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("FR", "France"), franceIossNumberRegex, "This will start with IM250 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("DE", "Germany"), germanyIossNumberRegex, "This will start with IM276 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("EL", "Greece"), greeceIossNumberRegex, "This will start with IM300 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("HU", "Hungary"), hungaryIossNumberRegex, "This will start with IM348 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("IE", "Ireland"), irelandIossNumberRegex, "This will start with IM372 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("IT", "Italy"), italyIossNumberRegex, "This will start with IM380 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("LV", "Latvia"), latviaIossNumberRegex, "This will start with IM428 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("LT", "Lithuania"), lithuaniaIossNumberRegex, "This will start with IM440 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("LU", "Luxembourg"), luxembourgIossNumberRegex, "This will start with IM442 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("MT", "Malta"), maltaIossNumberRegex, "This will start with IM470 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("NL", "Netherlands"), netherlandsIossNumberRegex, "This will start with IM528 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("PL", "Poland"), polandIossNumberRegex, "This will start with IM616 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("PT", "Portugal"), portugalIossNumberRegex, "This will start with IM620 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("RO", "Romania"), romaniaIossNumberRegex, "This will start with IM642 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("SK", "Slovakia"), slovakiaIossNumberRegex, "This will start with IM703 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("SI", "Slovenia"), sloveniaIossNumberRegex, "This will start with IM705 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("ES", "Spain"), spainIossNumberRegex, "This will start with IM724 followed by 7 numbers"),
    IossRegistrationNumberValidation(Country("SE", "Sweden"), swedenIossNumberRegex, "This will start with IM752 followed by 7 numbers")
  )

  private val austriaIossNumberRegex       = """^IM040[0-9]{7}$"""
  private val belgiumIossNumberRegex       = """^IM056[0-9]{7}$"""
  private val bulgariaIossNumberRegex      = """^IM100[0-9]{7}$"""
  private val cyprusIossNumberRegex        = """^IM196[0-9]{7}$"""
  private val czechRepublicIossNumberRegex = """^IM203[0-9]{7}$"""
  private val germanyIossNumberRegex       = """^IM276[0-9]{7}$"""
  private val denmarkIossNumberRegex       = """^IM208[0-9]{7}$"""
  private val estoniaIossNumberRegex       = """^IM233[0-9]{7}$"""
  private val greeceIossNumberRegex        = """^IM300[0-9]{7}$"""
  private val spainIossNumberRegex         = """^IM724[0-9]{7}$"""
  private val finlandIossNumberRegex       = """^IM246[0-9]{7}$"""
  private val franceIossNumberRegex        = """^IM250[0-9]{7}$"""
  private val croatiaIossNumberRegex       = """^IM191[0-9]{7}$"""
  private val hungaryIossNumberRegex       = """^IM348[0-9]{7}$"""
  private val irelandIossNumberRegex       = """^IM372[0-9]{7}$"""
  private val italyIossNumberRegex         = """^IM380[0-9]{7}$"""
  private val lithuaniaIossNumberRegex     = """^IM440[0-9]{7}$"""
  private val luxembourgIossNumberRegex    = """^IM442[0-9]{7}$"""
  private val latviaIossNumberRegex        = """^IM428[0-9]{7}$"""
  private val maltaIossNumberRegex         = """^IM470[0-9]{7}$"""
  private val netherlandsIossNumberRegex   = """^IM528[0-9]{7}$"""
  private val polandIossNumberRegex        = """^IM616[0-9]{7}$"""
  private val portugalIossNumberRegex      = """^IM620[0-9]{7}$"""
  private val romaniaIossNumberRegex       = """^IM642[0-9]{7}$"""
  private val swedenIossNumberRegex        = """^IM752[0-9]{7}$"""
  private val sloveniaIossNumberRegex      = """^IM705[0-9]{7}$"""
  private val slovakiaIossNumberRegex      = """^IM703[0-9]{7}$"""
}