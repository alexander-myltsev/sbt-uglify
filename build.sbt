sbtPlugin := true

name := "sbt-uglify"

version := "0.0.1"

organization := "name.myltsev"

Seq(scriptedSettings:_*)

scriptedLaunchOpts <+= version { "-Dplugin.version=" + _ }
