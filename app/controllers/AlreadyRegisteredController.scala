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

package controllers

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions._
import formats.Format.dateFormatter
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository
import services.DateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AlreadyRegisteredView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AlreadyRegisteredController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             view: AlreadyRegisteredView,
                                             connector: RegistrationConnector,
                                             dateService: DateService,
                                             sessionRepository: SessionRepository,
                                             config: FrontendAppConfig
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      for {
        sessionData <- sessionRepository.get(request.userId)
        registrationData <- connector.getRegistration()
      } yield {
        registrationData match {
          case Some(registration) =>
            val commencementDate = registration.commencementDate
            val dateOfFirstSale = registration.dateOfFirstSale
            val vatReturnEndDate = dateService.getVatReturnEndDate(commencementDate)
            val vatReturnDeadline = dateService.getVatReturnDeadline(vatReturnEndDate)
            val isDOFSDifferentToCommencementDate =
              dateService.isDOFSDifferentToCommencementDate(dateOfFirstSale, commencementDate)
            val savedUrl = sessionData.headOption.flatMap(_.get[String](ExternalReturnUrlQuery.path))

            Ok(
              view(
                HtmlFormat.escape(registration.registeredCompanyName).toString,
                request.vrn,
                config.feedbackUrl,
                commencementDate.format(dateFormatter),
                vatReturnEndDate.format(dateFormatter),
                vatReturnDeadline.format(dateFormatter),
                dateService.lastDayOfCalendarQuarter.format(dateFormatter),
                dateService.startOfCurrentQuarter.format(dateFormatter),
                dateService.startOfNextQuarter.format(dateFormatter),
                isDOFSDifferentToCommencementDate,
                savedUrl
              )
            )

          case None =>
            Redirect(routes.IndexController.onPageLoad())
        }
      }
  }
}