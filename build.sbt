import bintray.Keys._

sbtPlugin := true

name := "sbt-uglify"

version := "0.0.1-SNAPSHOT-1e78db8c6e7de8ba931ae27de3a3515531f3075b"

organization := "name.myltsev"

Seq(scriptedSettings:_*)

scriptedLaunchOpts <+= version { "-Dplugin.version=" + _ }

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.2" % "test"

licenses in GlobalScope += "Apache-2.0" -> url("https://github.com/eltimn/sbt-closure/raw/master/LICENSE")

Seq(bintrayPublishSettings: _*) ++ Seq(
  publishMavenStyle := false,
  bintrayOrganization in bintray := None,
  repository in bintray := "sbt-plugins"
)
