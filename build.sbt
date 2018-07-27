name := """ot-geckoapi"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.9"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"
libraryDependencies += "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.1.40"
libraryDependencies += "org.sangria-graphql" %% "sangria" % "1.4.1"
// libraryDependencies += "org.sangria-graphql" %% "sangria-relay" % "1.4.1"
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "1.0.4"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.opentargets.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.opentargets.binders._"
