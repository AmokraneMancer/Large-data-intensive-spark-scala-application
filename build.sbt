scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
)

libraryDependencies ++= Seq(
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8", // for visualization
  "org.apache.spark" %% "spark-sql" % "2.4.3",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)
