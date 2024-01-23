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

package controllers.amend

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions._
import controllers.routes
import models.UserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.AmendCompleteView

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AmendCompleteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         view: AmendCompleteView,
                                         frontendAppConfig: FrontendAppConfig,
                                         registrationConnector: RegistrationConnector,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = (cc.actionBuilder andThen cc.identify andThen cc.getData andThen cc.requireData(isInAmendMode = false)).async {
    implicit request => {

      for {
        externalEntryUrl <- registrationConnector.getSavedExternalEntry()
      } yield {
        {
          for {
            organisationName <- getOrganisationName(request.userAnswers)
          } yield {
            val savedUrl = externalEntryUrl.fold(_ => None, _.url)
            Ok(
              view(
                request.vrn,
                frontendAppConfig.feedbackUrl,
                savedUrl,
                frontendAppConfig.iossYourAccountUrl,
                organisationName
              )
            )
          }
        }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }

  private def getOrganisationName(answers: UserAnswers): Option[String] =
    answers.vatInfo match {
      case Some(vatInfo) if (vatInfo.organisationName.isDefined) => vatInfo.organisationName
      case Some(vatInfo) if (vatInfo.individualName.isDefined) => vatInfo.individualName
      case _ => None
    }
}
