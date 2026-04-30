package repositories

import com.typesafe.config.Config
import config.FrontendAppConfig
import crypto.UserAnswersEncryptor
import models.{EncryptedUserAnswers, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import services.crypto.EncryptionService
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticatedUserAnswersRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedUserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

  private val userAnswers: UserAnswers = UserAnswers("id", Json.obj("foo" -> "bar"), None, Instant.ofEpochSecond(1))

  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockEncryptionService: EncryptionService = new EncryptionService(mockConfiguration)
  private val encryptor = new UserAnswersEncryptor(mockAppConfig, mockEncryptionService)
  private val secretKey: String = "VqmXp7yigDFxbCUdDdNZVIvbW6RgPNJsliv6swQNCL8="

  protected override val repository: AuthenticatedUserAnswersRepository = new AuthenticatedUserAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    encryptor = encryptor,
    clock = stubClock
  )

  when(mockConfiguration.underlying) thenReturn mockConfig
  when(mockConfig.getString(any())) thenReturn secretKey
  when(mockAppConfig.encryptionKey) thenReturn secretKey

  ".set" - {

    "must set the lastUpdated time on the supplied user answers to 'now', then save them" in {

      val expectedResult = userAnswers.copy(lastUpdated = stubClock.instant())
      val encryptedExpectedResult = encryptor.encryptUserAnswers(expectedResult)

      val setResult = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

      setResult mustBe true
      updatedRecord mustBe encryptedExpectedResult
    }
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and return the record" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

        val result = repository.get(userAnswers.id).futureValue

        val expectedResult = userAnswers.copy(lastUpdated = stubClock.instant())

        result.value mustBe expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time to 'now', then return true" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

        val result = repository.keepAlive(userAnswers.id).futureValue

        val expectedResult = userAnswers.copy(lastUpdated = stubClock.instant())
        val encryptedExpectedUpdatedAnswers = encryptor.encryptUserAnswers(expectedResult)

        result mustBe true
        val updatedAnswers = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

        updatedAnswers mustBe encryptedExpectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustBe true
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

      val result = repository.clear(userAnswers.id).futureValue

      result mustBe true
      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {

      val result = repository.clear("id that does not exist").futureValue
      result mustBe true
    }
  }

}
