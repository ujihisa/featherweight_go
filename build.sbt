import ReleaseTransformations._
import UpdateReadme.updateReadme

val scala213Version = "2.13.2"
val defaultScalacOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-Xlint",
  "-language:implicitConversions", "-language:higherKinds", "-language:existentials",
  "-unchecked"
)

lazy val root = project.in(file("."))
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
  .settings(publishSettings)
  .aggregate(
    featherweightGoCoreJVM,
    featherweightGoCoreJS,
    featherweightGoJS,
    featherweightGoJVM
  )

lazy val featherweightGo = (crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Full) in file("."))
  .settings(
    scalaVersion := scala213Version,
    scalacOptions ++= defaultScalacOptions,
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    libraryDependencies += "com.lihaoyi" %%% "pprint" % "0.5.9"
  )
  .settings(publishSettings)
  .dependsOn(
    featherweightGoCore
  )
  .jsSettings(
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/y-yu/featherweight_go/" + tagOrHash.value
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
  )
  .jvmSettings(
    mainClass := Some("featherweightgo.Main")
  )

lazy val featherweightGoCore = (crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure) in file("./core"))
  .settings(
    organization := "com.github.y-yu",
    name := "featherweight_go",
    description := "Scala implementation of Featherweight Go",
    homepage := Some(url("https://github.com/y-yu")),
    licenses := Seq("MIT" -> url(s"https://github.com/y-yu/featherweight_go/blob/master/LICENSE")),
    scalaVersion := scala213Version,
    scalacOptions ++= defaultScalacOptions,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.2",
      "org.scalatest" %%% "scalatest" % "3.1.2" % "test"
    )
  )
  .settings(publishSettings)

lazy val featherweightGoCoreJVM = featherweightGoCore.jvm
lazy val featherweightGoCoreJS = featherweightGoCore.js

lazy val featherweightGoJVM = featherweightGo.jvm
lazy val featherweightGoJS = featherweightGo.js

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  publishArtifact in Test := false,
  pomExtra :=
    <developers>
      <developer>
        <id>y-yu</id>
        <name>Yoshimura Hikaru</name>
        <url>https://github.com/y-yu</url>
      </developer>
    </developers>
      <scm>
        <url>git@github.com:y-yu/featherweight_go.git</url>
        <connection>scm:git:git@github.com:y-yu/featherweight_go.git</connection>
        <tag>{tagOrHash.value}</tag>
      </scm>,
  releaseTagName := tagName.value,
  releaseCrossBuild := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    updateReadme,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("^ publishSigned"),
    setNextVersion,
    updateReadme,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )
)

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}
