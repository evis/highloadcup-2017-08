name := "highloadcup2017"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "2.17.1",
  "io.spray" %%  "spray-json" % "1.3.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "org.rapidoid" % "rapidoid-http-fast" % "5.4.2"
)

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(
    sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName).mkString(":") + ":" + jarTarget
  new Dockerfile {
    // Base image
    from("java")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // Expose HTTP server port
    expose(80)
    // On launch run Java with the classpath and the main class
    entryPoint("java",
      "-XX:+UseConcMarkSweepGC",
      "-XX:+CMSIncrementalMode",
      "-Xms4g",
      "-Xmx4g",
      "-cp", classpathString, mainclass, "80")
  }
}

imageNames in docker := Seq(ImageName("stor.highloadcup.ru/travels/ultra_chinook"))
