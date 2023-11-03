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
import controllers.actions._
import formats.Format.{dateFormatter, dateMonthYearFormatter}
import models.UserAnswers
import pages.{EmptyWaypoints, JourneyRecoveryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.etmp.EtmpEnrolmentResponseQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApplicationCompleteView

import java.time.{Clock, LocalDate}
import javax.inject.Inject


class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               view: ApplicationCompleteView,
                                               frontendAppConfig: FrontendAppConfig,
                                               clock: Clock
                                             ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] =  (cc.actionBuilder andThen cc.identify andThen cc.getData andThen cc.requireData()) {
    implicit request =>

      (for {
        etmpEnrolmentResponse <- request.userAnswers.get(EtmpEnrolmentResponseQuery)
        organisationName <- getOrganisationName(request.userAnswers)
      } yield {

        val iossReferenceNumber = etmpEnrolmentResponse.iossReference

        val commencementDate = LocalDate.now(clock)
        val returnStartDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
        val includedSalesDate = commencementDate.withDayOfMonth(1)

        Ok(view(
          iossReferenceNumber,
          organisationName,
          includedSalesDate.format(dateMonthYearFormatter),
          returnStartDate.format(dateFormatter),
          includedSalesDate.format(dateFormatter),
          frontendAppConfig.feedbackUrl
        ))
      }).getOrElse(Redirect(JourneyRecoveryPage.route(EmptyWaypoints)))
  }

  private def getOrganisationName(answers: UserAnswers): Option[String] =
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => None
    }
}
