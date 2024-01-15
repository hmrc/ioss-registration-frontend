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

package controllers.rejoin

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions._
import models.UserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.rejoin.NewIossReferenceQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.rejoin.RejoinCompleteView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RejoinCompleteController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          view: RejoinCompleteView,
                                          frontendAppConfig: FrontendAppConfig,
                                          registrationConnector: RegistrationConnector,
                                          clock: Clock
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = (cc.actionBuilder andThen cc.identify andThen cc.getData andThen cc.requireData(isInAmendMode = true)).async {
    implicit request => {
      registrationConnector.getSavedExternalEntry().map { externalEntryUrl =>
        val newIossReference = getNewIossReference(request.userAnswers)
        val organisationName = getOrganisationName(request.userAnswers)
        val savedUrl = externalEntryUrl.fold(_ => None, _.url)

        val commencementDate = LocalDate.now(clock)
        val returnStartDate: LocalDate = commencementDate.withDayOfMonth(commencementDate.lengthOfMonth()).plusDays(1)
        val includedSalesDate = commencementDate.withDayOfMonth(1)

        Ok(
          view(
            vrn = request.vrn,
            feedbackLink = frontendAppConfig.feedbackUrl,
            externalUrl = savedUrl,
            yourAccountUrl = frontendAppConfig.iossYourAccountUrl,
            organisationName = organisationName,
            newIossReference = newIossReference,
            commencementDate = commencementDate,
            returnStartDate = returnStartDate,
            includedSalesDate = includedSalesDate
          )
        )
      }
    }
  }

  private def getNewIossReference(answers: UserAnswers) = {
    answers.get(NewIossReferenceQuery).getOrElse(throw new RuntimeException("NewIossReference has not been set in answers"))
  }

  private def getOrganisationName(answers: UserAnswers): String = {
    answers.vatInfo.flatMap { vatInfo =>
      vatInfo.organisationName.fold(vatInfo.individualName)(Some.apply)
    }.getOrElse(throw new RuntimeException("OrganisationName has not been set in answers"))
  }
}
