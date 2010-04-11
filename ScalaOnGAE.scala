import sbt._
import Process._
import sbt.{FileUtilities=>fu}

class ScalaOnGAE(info: ProjectInfo) extends DefaultWebProject(info)
{
    def appengineVertion = "1.3.2"
    override def libraryDependencies = Set (
      "com.google.appengine" % "appengine-java-sdk" % appengineVertion % "test",
      "com.google.appengine" % "appengine-api-labs" % appengineVertion,
      "com.google.appengine" % "appengine-api-1.0-sdk" % appengineVertion,
      "com.google.appengine" % "datanucleus-appengine" % "1.0.5",
      "com.google.appengine" % "datanucleus-core" % "1.1.5",
      "com.google.appengine" % "datanucleus-jpa" % "1.1.5",
      "com.google.appengine" % "geronimo-jpa_3.0_spec" % "1.1.1",
      "com.google.appengine" % "geronimo-jta_1.1_spec" % "1.1.1",
      "com.google.appengine" % "jdo2-api" % "2.3-eb",
      "org.scala-tools.testing" % "specs" % "1.6.1",
      "jetty" % "servlet-api" % "2.5-6.0.0" 
    ) ++ super.libraryDependencies

    val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
    val appengineRepository = "Google App Engine Repository" at "http://maven-gae-plugin.googlecode.com/svn/repository"

    def gitLocation:String = "git://github.com/koduki/scala_on_gae.git"
    def saveDir:Path = Path.fromFile(System.getProperty("user.home")) / ".scala_on_gae" 

    lazy val syncSog = syncSOGTask 
    def syncSOGTask:Task = task{
        val git = (if(!saveDir.exists && !saveDir.isDirectory){ 
                        fu.createDirectory(saveDir, log) 
                        List("git", "clone", gitLocation, ".") 
                    } else {
                        List("git", "pull", gitLocation, "master")
                    }).mkString(" ")
                    
        log.info("Syncing " + gitLocation  + " with " + saveDir)
        apply(git, saveDir) ! log
        val sourcePath = (saveDir / "src").absolutePath
        val destPath = info.projectPath.absolutePath  

        apply(Array("cp","-rf", sourcePath, destPath))! log
        log.info("Copied " + sourcePath  + " to " + destPath)
        None
    } 

    lazy val runServer= runServerTask
    def runServerTask:Task = task {
        val testLibPath = Path.fromFile(managedDependencyPath.absolutePath ) / "test"
        fu.unzip(testLibPath/("appengine-java-sdk-" + appengineVertion +  ".zip"), testLibPath, log)
        apply(Array("/bin/sh",(testLibPath/("appengine-java-sdk-" + appengineVertion)/"bin"/"dev_appserver.sh").absolutePath, temporaryWarPath.absolutePath)).lines_!
        None
    }

    lazy val cleanUpdate = update dependsOn(clean) describedAs("clean and dependency update")
    lazy val cleanUpdateSync = syncSog dependsOn(cleanUpdate) describedAs("clean, dependency update and sync scala_on_gae framework")
    lazy val cleanUpdateSyncPrepare = prepareWebapp dependsOn(cleanUpdateSync) describedAs("clean, dependency update, sync scala_on_gae framework, compile sources and prepare webapp")
    lazy val build = runServer dependsOn(cleanUpdateSyncPrepare) describedAs("clean, dependency update, sync scala_on_gae framework, compile source, prepare webapp and run local GAE server")

    lazy val path = task{
      log.info("dependencyPath: " + dependencyPath.absolutePath)
      log.info("managedDependencyPath: " + managedDependencyPath.absolutePath)
      log.info("mainScalaSourcePath: " + mainScalaSourcePath.absolutePath)
      log.info("mainJavaSourcePath: " + mainJavaSourcePath.absolutePath)
      log.info("mainResourcesPath: " + mainResourcesPath.absolutePath)
      log.info("testScalaSourcePath: " + testScalaSourcePath.absolutePath)
      log.info("testJavaSourcePath: " + testJavaSourcePath.absolutePath)
      log.info("testResourcesPath: " + testResourcesPath.absolutePath)
      log.info("outputPath: " + outputPath.absolutePath)
      log.info("  mainDocPath: " + mainDocPath.absolutePath)
      log.info("  testDocPath: " + testDocPath.absolutePath)
      log.info("  mainCompilePath: " + mainCompilePath.absolutePath)
      log.info("  testCompilePath: " + testCompilePath.absolutePath)
      log.info("  mainAnalysisPath: " + mainAnalysisPath.absolutePath)
      log.info("  testAnalysisPath: " + testAnalysisPath.absolutePath)
      log.info("webappPath: " + webappPath.absolutePath)
      log.info("temporaryWarPath: " + temporaryWarPath.absolutePath)
      log.info("")
      log.info("docClasspath")
      compileClasspath.getPaths.toList.sort(_<_).foreach(path => log.info("  "+path))
      log.info("compileClasspath")
      compileClasspath.getPaths.toList.sort(_<_).foreach(path => log.info("  "+path))
      log.info("testClasspath")
      testClasspath.getPaths.toList.sort(_<_).foreach(path => log.info("  "+path))
      log.info("runClasspath")
      runClasspath.getPaths.toList.sort(_<_).foreach(path => log.info("  "+path))
      log.info("consoleClasspath")
      consoleClasspath.getPaths.toList.sort(_<_).foreach(path => log.info("  "+path))
      None
    } describedAs ("Show path variable of this project")

}


