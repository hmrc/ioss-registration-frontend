import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "7.19.0"
  private val hmrcMongoVersion = "1.3.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "7.14.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"             % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "domain"                         % "8.3.0-play-28",
    "org.typelevel"     %% "cats-core"                      % "2.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "org.scalatest"           %% "scalatest"               % "3.2.15",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-3-4"             % "3.2.10.0",
    "org.mockito"             %% "mockito-scala"           % "1.17.12",
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.15.4",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.6",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "2.27.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
