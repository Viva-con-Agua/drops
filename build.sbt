import com.typesafe.sbt.packager.docker.Cmd

name := """Drops"""

version := "0.36.63"

lazy val root = (project in file(".")).enablePlugins(PlayScala).enablePlugins(DockerPlugin)

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  Resolver.sonatypeRepo("snapshots")
 )

pipelineStages := Seq(digest,gzip)

libraryDependencies ++= Seq(
  cache,
  filters,
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "com.mohiva" %% "play-silhouette" % "3.0.0",
  "org.webjars" %% "webjars-play" % "2.4.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P24-B3",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.16.1",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
	"com.github.tyagihas" % "scala_nats_2.11" % "0.3.0",
  "io.nats" % "jnats" % "2.4.2",
  specs2 % Test
)

routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override",
  "-Ywarn-value-discard",
  "-language:reflectiveCalls"
)

//includeFilter in (Assets, LessKeys.less) := "vca.less" // | "bar.less"
includeFilter in (Assets, LessKeys.less) := "*.less"
excludeFilter in (Assets, LessKeys.less) := "_*.less"

// setting a maintainer which is used for all packaging types</pre>
maintainer in Docker := "Johann Sell"

// exposing the play ports
dockerExposedPorts := Seq(9000, 9443)

dockerRepository := Some("vivaconagua")
version in Docker := "latest"

