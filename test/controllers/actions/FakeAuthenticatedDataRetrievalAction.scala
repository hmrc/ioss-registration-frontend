package controllers.actions

import controllers.actions.FakeAuthenticatedDataRetrievalAction.{mockMigrationService, mockSessionRepository}
import models.UserAnswers
import models.requests.{AuthenticatedIdentifierRequest, AuthenticatedOptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.domain.Vrn
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthenticatedDataRetrievalAction(dataToReturn: Option[UserAnswers], vrn: Vrn)
extends AuthenticatedDataRetrievalAction(mockSessionRepository, mockMigrationService)(ExecutionContext.Implicits.global) {

  override protected def refine[A](request: AuthenticatedIdentifierRequest[A]): Future[Either[Result, AuthenticatedOptionalDataRequest[A]]] =
    Right(
      AuthenticatedOptionalDataRequest(
        request,
        request.credentials,
        request.vrn,
        dataToReturn
      )
    ).toFuture
}

object FakeAuthenticatedDataRetrievalAction {
  val mockSessionRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  val mockMigrationService: DataMigrationService = mock[DataMigrationService]
}
