val scala3Version = "3.4.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "global-processing-engine",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0-M11" % Test,
    libraryDependencies += "com.typesafe" % "config" % "1.4.3",
    libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.20.0"
  )
