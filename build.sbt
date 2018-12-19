name := "tt_poc_containers"
version := "0.1"
scalaVersion := "2.11.11"

//lazy val dockerComposeTag = "DockerComposeTag"

enablePlugins(DockerComposePlugin)

composeFile := baseDirectory.value + "/docker/docker-compose.yml"
testTagsToExecute := "DockerComposeTag"


//testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-l", dockerComposeTag)



libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "2.1.0","com.typesafe.slick" %% "slick-extensions" % "2.1.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.akka" %% "akka-actor" % "2.4.4" force(), "com.typesafe.akka" %% "akka-cluster" % "2.4.4", "com.typesafe.akka" %% "akka-stream" % "2.4.4","com.typesafe.akka" %% "akka-cluster-tools" % "2.4.4",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0","io.spray" %% "spray-json" % "1.3.2","com.typesafe.akka" %% "akka-http-core-experimental" % "1.0",
  "c3p0" % "c3p0" % "0.9.1.2","joda-time" % "joda-time" % "2.8.2",
  "nl.grons" %% "metrics-scala" % "3.5.2", "com.h2database" % "h2" % "1.4.197" % Test,
  "org.scala-stm" %% "scala-stm" % "0.7","org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % Test, "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,"com.typesafe.akka" %% "akka-slf4j" % "2.4.4", "com.microsoft.sqlserver" % "mssql-jdbc" % "6.1.0.jre7" % Test)

dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % "2.4.4"

