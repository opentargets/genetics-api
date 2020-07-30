import com.typesafe.sbt.packager.MappingsHelper._

name := """ot-genetics-api"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"
maintainer := "ops@opentargets.org"

javacOptions ++= Seq("-encoding", "UTF-8")

scalacOptions in ThisBuild ++= Seq("-language:_", "-Ypartial-unification", "-Xfatal-warnings")

// include resources into the unversal zipped package
mappings in Universal ++= directory(baseDirectory.value / "resources")

resolvers += Resolver.sonatypeRepo("releases")

val playVersion = "2.8.1"
val elastic4scalaVersion = "7.7.0"
libraryDependencies ++= Seq(
  guice,
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test,
  "com.typesafe.play" %% "play" % playVersion,
  "com.typesafe.play" %% "play-logback" % playVersion,
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.2.4",
  "org.sangria-graphql" %% "sangria" % "2.0.0",
  "org.sangria-graphql" %% "sangria-play-json" % "2.0.1",
  "com.nrinaudo" %% "kantan.csv" % "0.4.0",
  "com.nrinaudo" %% "kantan.csv-generic" % "0.4.0",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4scalaVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4scalaVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4scalaVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % elastic4scalaVersion)
