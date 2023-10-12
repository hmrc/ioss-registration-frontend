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

package controllers.test

import connectors.test.TestOnlyEmailPasscodeConnector
import controllers.actions.AuthenticatedControllerComponents
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.EmailVerificationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyEmailPasscodeController @Inject()(
                                                 cc: AuthenticatedControllerComponents,
                                                 testOnlyEmailPasscodeConnector: TestOnlyEmailPasscodeConnector,
                                                 emailVerificationService: EmailVerificationService
                                               )(implicit ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {

  def testOnlyGetPasscodes(): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      testOnlyEmailPasscodeConnector.getTestOnlyPasscode().flatMap {
        case Right(response) =>
          val firstValue = response.passcodes.headOption.map(_.passcode).getOrElse("")
          Future.successful(Ok(s"""<p id="testOnlyPasscodes">$response</p><p id="testOnlyPasscode">${firstValue}</p>"""))
        case Left(error) => throw error
      }
  }

  def testOnlyGetEmailVerificationStatus(): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      emailVerificationService.getStatus(request.userId).map {
        case Right(Some(verificationStatus)) => Ok(Json.toJson(verificationStatus))
        case Right(None) => Ok(Json.toJson("None"))
        case Left(error) => InternalServerError(error.body)
      }
  }
}
