/*
 * Copyright 2024 HM Revenue & Customs
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

package models.amend

import formats.Format.dateMonthYearFormatter
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.LocalDate

final case class PreviousRegistration(iossNumber: String, startPeriod: LocalDate, endPeriod: LocalDate)

object PreviousRegistration {

  implicit val format: OFormat[PreviousRegistration] = Json.format[PreviousRegistration]

  def options(previousRegistrations: Seq[PreviousRegistration])(implicit messages: Messages): Seq[RadioItem] = {
    previousRegistrations.zipWithIndex.map {
      case (previousRegistration, index) =>
        val startPeriod: String = previousRegistration.startPeriod.format(dateMonthYearFormatter)
        val endPeriod: String = previousRegistration.endPeriod.format(dateMonthYearFormatter)

        RadioItem(
          content = Text(s"$startPeriod to $endPeriod"),
          id = Some(s"value_$index"),
          value = Some(previousRegistration.iossNumber),
          hint = Some(Hint(content = HtmlContent(messages("viewOrChangePreviousRegistrationsMultiple.hint", previousRegistration.iossNumber))))
        )
    }
  }
}
