sbtPlugin := true

name := "sbt-uglify"

version := "0.0.1"

organization := "name.myltsev"

Seq(scriptedSettings:_*)

scriptedLaunchOpts <+= version { "-Dplugin.version=" + _ }

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.2" % "test"
