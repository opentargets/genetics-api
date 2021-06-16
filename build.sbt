import com.typesafe.sbt.packager.MappingsHelper._

name := """ot-genetics-api"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.12"
maintainer := "ops@opentargets.org"

javacOptions ++= Seq("-encoding", "UTF-8")

scalacOptions in ThisBuild ++= Seq("-language:_", "-Ypartial-unification", "-Xfatal-warnings")

// include resources into the unversal zipped package
mappings in Universal ++= directory(baseDirectory.value / "resources")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  guice,
  "com.github.pathikrit" %% "better-files" % "3.9.1",
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.8.0" % Test,
  "com.nrinaudo" %% "kantan.csv" % "0.4.0",
  "com.nrinaudo" %% "kantan.csv-generic" % "0.4.0",
)

val playVersion = "2.8.8"
libraryDependencies += "com.typesafe.play" %% "play" % playVersion
libraryDependencies += "com.typesafe.play" %% "filters-helpers" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-logback" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"
libraryDependencies += "com.typesafe.play" %% "play-streams" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"

val sangriaVersion = "2.1.3"
libraryDependencies += "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.3.1"
libraryDependencies += "org.sangria-graphql" %% "sangria" % sangriaVersion
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "2.0.1"

val s4sVersion = "7.12.2"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % s4sVersion
)