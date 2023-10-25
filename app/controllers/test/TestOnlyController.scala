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

import connectors.test.TestOnlyConnector
import controllers.actions.UnauthenticatedControllerComponents
import models.external.ExternalRequest
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyController @Inject()(
                                    testConnector: TestOnlyConnector,
                                    cc: UnauthenticatedControllerComponents
                                  )(implicit ec: ExecutionContext) extends FrontendController(cc) {

  private val externalRequest = ExternalRequest("BTA", "/business-account")

  def enterFromExternal(lang: Option[String] = None): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      testConnector.externalEntry(externalRequest, lang).map {
        response =>
          response.fold(error => InternalServerError(error.body), r => Redirect(r.redirectUrl))
      }

  }

}