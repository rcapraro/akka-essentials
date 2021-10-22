name := "akka-essentials"

version := "0.1"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.17"
val scalaTestVersion = "3.2.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
