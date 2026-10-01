/*
 * Copyright 2026 HM Revenue & Customs
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

import logging.Logging
import models.euDetails.EuDetails
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow

case class DeleteAllFixedEstablishmentsAsPartOfVatGroupRowViewModel(
                                                                     countryName: String,
                                                                     companyName: String
                                                                   )

class DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel(val table: Table)

object DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel extends Logging {

  def apply(allEuDetails: List[EuDetails]): DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel = {

    new DeleteAllFixedEstablishmentsAsPartOfVatGroupViewModel(buildTable(allEuDetails))
  }

  private def buildTableRows(
                              country: String,
                              companyName: String
                            ): Seq[TableRow] = {
    Seq(
      TableRow(
        content = HtmlContent(
          country
        ),
        classes = "govuk-!-font-weight-regular"
      ),
      TableRow(
        content = HtmlContent(
          companyName
        ),
        classes = "govuk-!-font-weight-regular"
      )
    )
  }

  private def buildTable(allEuDetails: List[EuDetails]): Table = {

    val rows = allEuDetails.map { euDetails =>
      euDetails.fixedEstablishmentTradingName.map { companyName =>
        buildTableRows(
          country = euDetails.euCountry.name,
          companyName = companyName
        )
      }.getOrElse {
        val errorMessage: String = "Fixed Establishment Trading Name was not available."
        logger.error(errorMessage)
        val exception: Exception = new Exception(errorMessage)
        throw exception
      }
    }

    Table(
      rows = rows,
      firstCellIsHeader = true
    )
  }
}

