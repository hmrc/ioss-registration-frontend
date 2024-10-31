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

import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIossRequest, AuthenticatedOptionalDataRequest}
import pages.Waypoints
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import repositories.AuthenticatedUserAnswersRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

sealed trait RegistrationModificationMode

case object NotModifyingExistingRegistration extends RegistrationModificationMode

sealed trait ModifyingExistingRegistrationMode extends RegistrationModificationMode

case object AmendingActiveRegistration extends ModifyingExistingRegistrationMode

case object RejoiningRegistration extends ModifyingExistingRegistrationMode

case object AmendingPreviousRegistration extends ModifyingExistingRegistrationMode

trait AuthenticatedControllerComponents extends MessagesControllerComponents {

  def actionBuilder: DefaultActionBuilder

  def sessionRepository: AuthenticatedUserAnswersRepository

  def identify: AuthenticatedIdentifierAction

  def getData: AuthenticatedDataRetrievalAction

  def requireData: AuthenticatedDataRequiredAction

  def limitIndex: MaximumIndexFilterProvider

  def checkOtherCountryRegistration: CheckOtherCountryRegistrationFilter

  def checkRegistration: CheckRegistrationFilterProvider

  def checkPreviousRegistration: CheckAmendPreviousRegistrationFilterProvider

  def checkEmailVerificationStatus: CheckEmailVerificationFilterProvider

  def retrieveSavedAnswers: SavedAnswersRetrievalActionProvider

  def checkBouncedEmail: CheckBouncedEmailFilterProvider

  def requireIoss: IossRequiredAction

  def authAndGetData(
                      registrationModificationMode: RegistrationModificationMode = NotModifyingExistingRegistration,
                      restrictFromPreviousRegistrations: Boolean = true
                    ): ActionBuilder[AuthenticatedDataRequest, AnyContent] = {
    val modifyingExistingRegistration = registrationModificationMode != NotModifyingExistingRegistration
    actionBuilder andThen
      identify andThen
      checkRegistration(registrationModificationMode) andThen
      checkPreviousRegistration(registrationModificationMode, restrictFromPreviousRegistrations) andThen
      getData andThen
      requireData(modifyingExistingRegistration) andThen
      checkOtherCountryRegistration(registrationModificationMode)
  }

  def authAndGetOptionalData(): ActionBuilder[AuthenticatedOptionalDataRequest, AnyContent] = {
    actionBuilder andThen
      identify andThen
      getData
  }

  def authAndGetDataAndCheckVerifyEmail(
                                         registrationModificationMode: RegistrationModificationMode = NotModifyingExistingRegistration,
                                         restrictFromPreviousRegistrations: Boolean = true,
                                         waypoints: Waypoints
                                       ): ActionBuilder[AuthenticatedDataRequest, AnyContent] =
    authAndGetData(registrationModificationMode, restrictFromPreviousRegistrations) andThen
      checkEmailVerificationStatus(registrationModificationMode != NotModifyingExistingRegistration, waypoints)

  def authAndRequireIoss(
                          modifyingExistingRegistrationMode: ModifyingExistingRegistrationMode,
                          restrictFromPreviousRegistrations: Boolean = true,
                          waypoints: Waypoints
                        ): ActionBuilder[AuthenticatedMandatoryIossRequest, AnyContent] = {
    authAndGetDataAndCheckVerifyEmail(modifyingExistingRegistrationMode, restrictFromPreviousRegistrations, waypoints) andThen
      requireIoss() andThen
      checkBouncedEmail()
  }
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
                                                               checkOtherCountryRegistration: CheckOtherCountryRegistrationFilter,
                                                               checkEmailVerificationStatus: CheckEmailVerificationFilterProvider,
                                                               retrieveSavedAnswers: SavedAnswersRetrievalActionProvider,
                                                               checkRegistration: CheckRegistrationFilterProvider,
                                                               requireIoss: IossRequiredAction,
                                                               checkBouncedEmail: CheckBouncedEmailFilterProvider,
                                                               checkPreviousRegistration: CheckAmendPreviousRegistrationFilterProvider
                                                             ) extends AuthenticatedControllerComponents
