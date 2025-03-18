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

package controllers.ossExclusions

import controllers.actions.AuthenticatedControllerComponents
import formats.Format.quarantinedOSSRegistrationFormatter
import logging.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.oss.OssExclusionsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ossExclusions.CannotRegisterQuarantinedTraderView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CannotRegisterQuarantinedTraderController @Inject()(
                                                           override val messagesApi: MessagesApi,
                                                           cc: AuthenticatedControllerComponents,
                                                           ossExclusionsService: OssExclusionsService,
                                                           view: CannotRegisterQuarantinedTraderView
                                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      ossExclusionsService.getOssExclusion(request.vrn.vrn).map {
        case Some(ossExcludedTrader) =>
          val excludeEndDate: String = ossExcludedTrader.effectiveDate.getOrElse {
            val exception = new IllegalStateException("Expected effective date")
            logger.error(s"Service was unable to retrieve effective date: ${exception.getMessage}", exception)
            throw exception
          }.plusYears(2).plusDays(1).format(quarantinedOSSRegistrationFormatter)

          Ok(view(excludeEndDate))
        
        case _ =>
          val exception = new IllegalStateException("Expected effective date")
          logger.error(s"Service was unable to retrieve effective date: ${exception.getMessage}", exception)
          throw exception
      }
  }
}
