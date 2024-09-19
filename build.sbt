libraryDependencies ++= Seq(
  "io.vavr" % "vavr" % "0.10.4",
  "dev.zio" %% "zio" % "2.1.9",
  "org.hamcrest" % "hamcrest" % "3.0" % Test,
  "me.grison" % "vavr-matchers" % "1.3" % Test,
  "de.tototec" % "de.tobiasroeser.lambdatest" % "0.8.0" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.9.1" % Test,
  "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test
)

version := "0.0.1-SNAPSHOT"

scalaVersion := "3.5.0"

javacOptions ++= Seq("--release", "21")
