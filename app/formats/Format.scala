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

package formats

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  val dateMonthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  val dateHintFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d M yyyy")

  val eisDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
    .withLocale(Locale.UK)
    .withZone(ZoneId.of("GMT"))

  val eisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val dateOfRegistrationFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val quarantinedOSSRegistrationFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

}
