import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "board"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.1-SNAPSHOT" exclude("com.typesafe.play", "play-iteratees")
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" ** "main.less"),
    requireJs += "main.js",
    requireJsShim += "main.js"
  )

}
