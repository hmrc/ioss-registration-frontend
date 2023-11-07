package models.requests

import models.UserAnswers
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn


case class AuthenticatedMandatoryIossRequest[A](
                                                 request: Request[A],
                                                 credentials: Credentials,
                                                 vrn: Vrn,
                                                 iossNumber: String,
                                                 userAnswers: UserAnswers
                                               ) extends WrappedRequest[A](request) {

  val userId: String = credentials.providerId

}
