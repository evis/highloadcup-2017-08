name := "highloadcup2017"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.github.pathikrit" %% "better-files" % "3.0.0",
  "io.spray" %%  "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
