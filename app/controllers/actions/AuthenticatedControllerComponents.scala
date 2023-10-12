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

package controllers.actions

import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest}
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import repositories.AuthenticatedUserAnswersRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuthenticatedControllerComponents extends MessagesControllerComponents {

  def actionBuilder: DefaultActionBuilder

  def sessionRepository: AuthenticatedUserAnswersRepository

  def identify: AuthenticatedIdentifierAction

  def getData: AuthenticatedDataRetrievalAction

  def requireData: AuthenticatedDataRequiredAction

  def limitIndex: MaximumIndexFilterProvider

  def checkEmailVerificationStatus: CheckEmailVerificationFilterProvider

  def authAndGetData(): ActionBuilder[AuthenticatedDataRequest, AnyContent] = {
    actionBuilder andThen
      identify andThen
      getData andThen
      requireData()
  }

  def authAndGetOptionalData(): ActionBuilder[AuthenticatedOptionalDataRequest, AnyContent] = {
    actionBuilder andThen
      identify andThen
      getData
  }

  def authAndGetDataAndCheckVerifyEmail(): ActionBuilder[AuthenticatedDataRequest, AnyContent] =
    authAndGetData() andThen
      checkEmailVerificationStatus()

}

case class DefaultAuthenticatedControllerComponents @Inject()(
                                                               messagesActionBuilder: MessagesActionBuilder,
                                                               actionBuilder: DefaultActionBuilder,
                                                               parsers: PlayBodyParsers,
                                                               messagesApi: MessagesApi,
                                                               langs: Langs,
                                                               fileMimeTypes: FileMimeTypes,
                                                               executionContext: ExecutionContext,
                                                               sessionRepository: AuthenticatedUserAnswersRepository,
                                                               identify: AuthenticatedIdentifierAction,
                                                               getData: AuthenticatedDataRetrievalAction,
                                                               requireData: AuthenticatedDataRequiredAction,
                                                               limitIndex: MaximumIndexFilterProvider,
                                                               checkEmailVerificationStatus: CheckEmailVerificationFilterProvider
                                                             ) extends AuthenticatedControllerComponents
