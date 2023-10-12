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

package config

import java.time.LocalDate

object Constants {

  val maxTradingNames: Int = 10
  val maxWebsites: Int = 10

  val tradingNameReservedWords: Set[String] = Set("limited", "ltd", "llp", "plc")
  val maxSchemes: Int = 3
  val lastSchemeForCountry: Int = 1
  val maxOssSchemes: Int = 2
  val maxIossSchemes: Int = 1
  val schemeStartDate: LocalDate = LocalDate.of(2021, 7, 1)
  val emailVerificationMaxEmails: Int = 10
}
