import sbt._ 
import Keys._

object ScalatraOpenIDProviderBuild extends Build {

  lazy val _scalatraVersion = "2.0.4"
  lazy val _servletApi = "javax.servlet" %  "servlet-api" % "2.5"

  //lazy val _scalatraVersion = "2.1.0-SNAPSHOT"
  //lazy val _servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1"

  lazy val openidProvider = Project(
    id = "main", 
    base = file("."), 
    settings = Defaults.defaultSettings ++ Seq(
      sbtPlugin := false,
      organization := "com.github.seratch",
      name := "scalatra-openid-provider-support",
      version := "2.0.0",
      scalaVersion := "2.9.2",
      crossScalaVersions := Seq("2.9.2", "2.9.1"),
      resolvers += "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
      resolvers += "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      // for openid4java-nodeps...
      resolvers += "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk",
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
        _scalatraDependencies ++ Seq(
          "org.openid4java"         %  "openid4java-server" % "[0.9,)",
          "com.weiglewilczek.slf4s" %  "slf4s_2.9.1"        % "1.0.7",
          "org.scalatest"           %% "scalatest"          % "1.7.2" % "test"
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
      scalaVersion := "2.9.2",
      resolvers ++= Seq(
        // for openid4java-nodeps...
        "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk",
        "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
        "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      ),
      libraryDependencies <++= (scalaVersion) { scalaVersion =>
         _scalatraDependencies ++ _containerDepenedencies ++ Seq(
          "com.github.seratch" %% "scalatra-thymeleaf-support" % "1.0.0",
          "com.github.seratch" %% "inputvalidator" % "[0.1,)",
          "ch.qos.logback" % "logback-classic" % "1.0.2" % "runtime"
        )
      },
      scalacOptions ++= Seq("-deprecation", "-unchecked")
    ) ++ _jettyOrbitHack
  ).dependsOn(openidProvider)

  lazy val _jettyOrbitHack = Seq(
    ivyXML := <dependencies>
      <exclude org="org.eclipse.jetty.orbit" />
    </dependencies>
  )

  lazy val _scalatraDependencies = Seq(
    "org.scalatra"      %  "scalatra_2.9.1" % _scalatraVersion,
    "org.scalatra"      %  "scalatra-scalatest_2.9.1" % _scalatraVersion % "test",
    "ch.qos.logback"    %  "logback-classic" % "1.0.2" % "runtime",
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


