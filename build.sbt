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
    entryPoint("java", "-cp", classpathString, mainclass, "80")
  }
}

imageNames in docker := Seq(ImageName("stor.highloadcup.ru/travels/ultra_chinook"))
