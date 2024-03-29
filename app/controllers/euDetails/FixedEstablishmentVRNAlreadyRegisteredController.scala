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

package controllers.euDetails

import controllers.GetCountry
import controllers.actions._
import models.Country
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.euDetails.FixedEstablishmentVRNAlreadyRegisteredView

import javax.inject.Inject

class FixedEstablishmentVRNAlreadyRegisteredController @Inject()(
                                                                  override val messagesApi: MessagesApi,
                                                                  cc: AuthenticatedControllerComponents,
                                                                  view: FixedEstablishmentVRNAlreadyRegisteredView
                                                                ) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryCode: String): Action[AnyContent] = {
    cc.authAndGetData(waypoints.registrationModificationMode) {
      implicit request =>
        Country.fromCountryCode(countryCode).map { country =>
          Ok(view(country.name))
        }.getOrElse(throw new RuntimeException(s"countryCode $countryCode not found"))
    }
  }

}
