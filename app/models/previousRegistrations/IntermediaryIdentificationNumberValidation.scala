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

case class IntermediaryIdentificationNumberValidation(country: Country, vrnRegex: String, messageInput: String)

object IntermediaryIdentificationNumberValidation {

  lazy val euCountriesWithIntermediaryValidationRules: Seq[IntermediaryIdentificationNumberValidation] = Seq(
    IntermediaryIdentificationNumberValidation(Country("AT", "Austria"),
      austriaIntermediaryIdentificationRegex, "This will start with IN040 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("BE", "Belgium"),
      belgiumIntermediaryIdentificationRegex, "This will start with IN056 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("BG", "Bulgaria"),
      bulgariaIntermediaryIdentificationRegex, "This will start with IN100 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("HR", "Croatia"),
      croatiaIntermediaryIdentificationRegex, "This will start with IN191 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("CY", "Cyprus"),
      cyprusIntermediaryIdentificationRegex, "This will start with IN196 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("CZ", "Czech Republic"),
      czechRepublicIntermediaryIdentificationRegex, "This will start with IN203 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("DK", "Denmark"),
      denmarkIntermediaryIdentificationRegex, "This will start with IN208 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("EE", "Estonia"),
      estoniaIntermediaryIdentificationRegex, "This will start with IN233 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("FI", "Finland"),
      finlandIntermediaryIdentificationRegex, "This will start with IN246 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("FR", "France"),
      franceIntermediaryIdentificationRegex, "This will start with IN250 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("DE", "Germany"),
      germanyIntermediaryIdentificationRegex, "This will start with IN276 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("EL", "Greece"),
      greeceIntermediaryIdentificationRegex, "This will start with IN300 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("HU", "Hungary"),
      hungaryIntermediaryIdentificationRegex, "This will start with IN348 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("IE", "Ireland"),
      irelandIntermediaryIdentificationRegex, "This will start with IN372 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("IT", "Italy"),
      italyIntermediaryIdentificationRegex, "This will start with IN380 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("LV", "Latvia"),
      latviaIntermediaryIdentificationRegex, "This will start with IN428 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("LT", "Lithuania"),
      lithuaniaIntermediaryIdentificationRegex, "This will start with IN440 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("LU", "Luxembourg"),
      luxembourgIntermediaryIdentificationRegex, "This will start with IN442 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("MT", "Malta"),
      maltaIntermediaryIdentificationRegex, "This will start with IN470 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("NL", "Netherlands"),
      netherlandsIntermediaryIdentificationRegex, "This will start with IN528 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("PL", "Poland"),
      polandIntermediaryIdentificationRegex, "This will start with IN616 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("PT", "Portugal"),
      portugalIntermediaryIdentificationRegex, "This will start with IN620 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("RO", "Romania"),
      romaniaIntermediaryIdentificationRegex, "This will start with IN642 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("SK", "Slovakia"),
      slovakiaIntermediaryIdentificationRegex, "This will start with IN703 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("SI", "Slovenia"),
      sloveniaIntermediaryIdentificationRegex, "This will start with IN705 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("ES", "Spain"),
      spainIntermediaryIdentificationRegex, "This will start with IN724 followed by 7 numbers"),
    IntermediaryIdentificationNumberValidation(Country("SE", "Sweden"),
      swedenIntermediaryIdentificationRegex, "This will start with IN752 followed by 7 numbers")
  )

  private val austriaIntermediaryIdentificationRegex        = """^IN040[0-9]{7}$"""
  private val belgiumIntermediaryIdentificationRegex        = """^IN056[0-9]{7}$"""
  private val bulgariaIntermediaryIdentificationRegex       = """^IN100[0-9]{7}$"""
  private val cyprusIntermediaryIdentificationRegex         = """^IN196[0-9]{7}$"""
  private val czechRepublicIntermediaryIdentificationRegex  = """^IN203[0-9]{7}$"""
  private val germanyIntermediaryIdentificationRegex        = """^IN276[0-9]{7}$"""
  private val denmarkIntermediaryIdentificationRegex        = """^IN208[0-9]{7}$"""
  private val estoniaIntermediaryIdentificationRegex        = """^IN233[0-9]{7}$"""
  private val greeceIntermediaryIdentificationRegex         = """^IN300[0-9]{7}$"""
  private val spainIntermediaryIdentificationRegex          = """^IN724[0-9]{7}$"""
  private val finlandIntermediaryIdentificationRegex        = """^IN246[0-9]{7}$"""
  private val franceIntermediaryIdentificationRegex         = """^IN250[0-9]{7}$"""
  private val croatiaIntermediaryIdentificationRegex        = """^IN191[0-9]{7}$"""
  private val hungaryIntermediaryIdentificationRegex        = """^IN348[0-9]{7}$"""
  private val irelandIntermediaryIdentificationRegex        = """^IN372[0-9]{7}$"""
  private val italyIntermediaryIdentificationRegex          = """^IN380[0-9]{7}$"""
  private val lithuaniaIntermediaryIdentificationRegex      = """^IN440[0-9]{7}$"""
  private val luxembourgIntermediaryIdentificationRegex     = """^IN442[0-9]{7}$"""
  private val latviaIntermediaryIdentificationRegex         = """^IN428[0-9]{7}$"""
  private val maltaIntermediaryIdentificationRegex          = """^IN470[0-9]{7}$"""
  private val netherlandsIntermediaryIdentificationRegex    = """^IN528[0-9]{7}$"""
  private val polandIntermediaryIdentificationRegex         = """^IN616[0-9]{7}$"""
  private val portugalIntermediaryIdentificationRegex       = """^IN620[0-9]{7}$"""
  private val romaniaIntermediaryIdentificationRegex        = """^IN642[0-9]{7}$"""
  private val swedenIntermediaryIdentificationRegex         = """^IN752[0-9]{7}$"""
  private val sloveniaIntermediaryIdentificationRegex       = """^IN705[0-9]{7}$"""
  private val slovakiaIntermediaryIdentificationRegex       = """^IN703[0-9]{7}$"""
}