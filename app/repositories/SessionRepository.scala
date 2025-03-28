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

package repositories

import config.FrontendAppConfig
import models.SessionData
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   appConfig: FrontendAppConfig,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext) extends PlayMongoRepository[SessionData](
  collectionName = "session-data",
  mongoComponent = mongoComponent,
  domainFormat   = SessionData.format,
  indexes        = Seq(
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIdx")
        .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
    ),
    IndexModel(
      Indexes.ascending("userId"),
      IndexOptions()
        .name("userIdIdx")
        .unique(true)
    )
  )
) {
  private def byUserId(userId: String): Bson = Filters.equal("userId", userId)

  def keepAlive(userId: String): Future[Boolean] =
    collection
      .updateMany(
        filter = byUserId(userId),
        update = Updates.set("lastUpdated", Instant.now(clock)),
      )
      .toFuture
      .map(_ => true)

  def get(userId: String): Future[Seq[SessionData]] =
    keepAlive(userId).flatMap {
      _ =>
        collection
          .find(byUserId(userId)).toFuture()
    }

  def set(sessionData: SessionData): Future[Boolean] = {

    val updatedSessionData = sessionData copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter      = byUserId(updatedSessionData.userId),
        replacement = updatedSessionData,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture
      .map(_.wasAcknowledged())
  }

  def clear(userId: String): Future[Boolean] =
    collection
      .deleteOne(byUserId(userId))
      .toFuture
      .map(_ => true)

}