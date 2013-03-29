import sbt._ 
import Keys._

object ScalatraOpenIDProviderBuild extends Build {

  lazy val _scalatraVersion = "2.2.0"
  lazy val _servletApi = "javax.servlet" %  "javax.servlet-api" % "3.0.1"

  lazy val openidProvider = Project(
    id = "main", 
    base = file("."), 
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := "com.github.seratch",
      name := "scalatra-openid-provider-support",
      version := "2.2.0",
      scalaVersion := "2.10.0",
      resolvers += "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
      resolvers += "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      // for openid4java-nodeps...
      resolvers += "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        _scalatraDependencies ++ Seq(
          "org.openid4java" %  "openid4java-server" % "[0.9,)",
          "org.scalatest"   %  "scalatest_2.10.0"   % "1.8"    % "test"
        )
      },
      publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
        else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { x => false },
      pomExtra := _commonPomExtra,
      scalacOptions ++= Seq("-deprecation", "-unchecked")
    ) ++ _jettyOrbitHack
  ) 

  lazy val openidProviderDemo = Project(
    id = "demo",
    base = file("demo"),
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := "com.github.seratch",
      name :=  "scalatra-openid-provider-support-demo",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.10.1",
      resolvers ++= Seq(
        // for openid4java-nodeps...
        "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk",
        "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
        "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      ),
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
         _scalatraDependencies ++ _containerDepenedencies ++ Seq(
          "com.github.seratch" %% "scalatra-thymeleaf-support" % "2.2.0",
          "com.github.seratch" %% "inputvalidator" % "[0.2,)",
          "ch.qos.logback"     % "logback-classic" % "1.0.11" % "runtime"
        )
      },
      scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
    ) ++ _jettyOrbitHack
  ).dependsOn(openidProvider)

  lazy val _jettyOrbitHack = Seq(
    ivyXML := <dependencies>
      <exclude org="org.eclipse.jetty.orbit" />
    </dependencies>
  )

  lazy val _scalatraDependencies = Seq(
    "org.scalatra"   %% "scalatra"           % _scalatraVersion,
    "org.scalatra"   %% "scalatra-scalatest" % _scalatraVersion % "test",
    "ch.qos.logback" %  "logback-classic"    % "1.0.11" % "runtime",
    _servletApi % "provided"
  )
  lazy val _containerDepenedencies = Seq(
    _servletApi %"container",
    "org.eclipse.jetty" % "jetty-webapp" % "7.6.4.v20120524" % "container" 
      exclude("org.eclipse.jetty.orbit", "javax.servlet")
  )

  lazy val _commonPomExtra = 
    <url>https://github.com/seratch/scalatra-openid-provider-support</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:seratch/scalatra-openid-provider-support.git</url>
      <connection>scm:git:git@github.com:seratch/scalatra-openid-provider-support.git</connection>
    </scm>
    <developers>
      <developer>
        <id>seratch</id>
        <name>Kazuhiro Sera</name>
        <url>https://github.com/seratch</url>
      </developer>
    </developers>

}


