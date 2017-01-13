import com.typesafe.sbt.packager.docker.Cmd

name := """Drops"""

version := "0.9.2"

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
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.16.1",
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

// setting a maintainer which is used for all packaging types</pre>
maintainer in Docker := "Johann Sell"

// exposing the play ports
dockerExposedPorts := Seq(9000, 9443)

dockerRepository := Some("cses")