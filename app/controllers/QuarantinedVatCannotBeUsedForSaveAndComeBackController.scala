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

package controllers

import config.Constants.addQuarantineYears
import controllers.actions.*
import formats.Format.dateFormatter
import models.Country
import pages.Waypoints

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.QuarantinedVatCannotBeUsedForSaveAndComeBackView

import java.time.LocalDate

class QuarantinedVatCannotBeUsedForSaveAndComeBackController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: QuarantinedVatCannotBeUsedForSaveAndComeBackView
                                     ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryCode: String, exclusionDate: String): Action[AnyContent] = (cc.actionBuilder andThen cc.identify) {
    implicit request =>

      val exclusionDateFormatted = LocalDate.parse(exclusionDate).plusYears(addQuarantineYears).format(dateFormatter)
      
      Ok(view(Country.getCountryName(countryCode), exclusionDateFormatted))
  }
}
