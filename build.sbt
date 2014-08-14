import bintray.Keys._

sbtPlugin := true

name := "sbt-uglify"

version := "0.0.1-SNAPSHOT-8eea60ceff36d532fc4a9ab291ecc210a14e2dd5"

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
