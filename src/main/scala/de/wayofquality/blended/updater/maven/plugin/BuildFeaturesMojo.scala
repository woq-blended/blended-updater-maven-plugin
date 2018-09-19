package de.wayofquality.blended.updater.maven.plugin

import java.io.File

import blended.updater.tools.configbuilder._
import org.apache.maven.BuildFailureException
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.{Component, Mojo, Parameter, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.apache.maven.project.artifact.AttachedArtifact

import scala.collection.JavaConverters._

@Mojo(name = "build-features", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
class BuildFeaturesMojo extends AbstractMojo {

  @Component
  var project: MavenProject = _

  @Parameter(property = "localRepositoryUrl")
  var localRepositoryUrl: String = _

  @Parameter(required = true, property = "srcFeatureDir")
  var srcFeatureDir: File = _

  @Parameter(defaultValue = "${project.build.directory}/features", property = "destFeatureDir")
  var destFeatureDir: File = _

  @Parameter(defaultValue = ".conf", property = "featureFileSuffix")
  var featureFileSuffix: String = _

  @Parameter(defaultValue = "true", property = "attachFeatures")
  var attach: Boolean = true

  @Parameter(defaultValue = "conf", property = "attachType")
  var attachType: String = _

  @Parameter(defaultValue = "false", property = "blended-updater.debug")
  var debug: Boolean = false

  /**
   * Resolve all artifacts with mvn URLs only from the dependencies of the project.
   */
  @Parameter(property = "resolveFromDependencies", defaultValue = "false")
  var resolveFromDependencies: Boolean = _

  override def execute() = {
    getLog.debug("Running Mojo build-features")

    //TODO
    //    val srcFeatureDir = new File(project.getBasedir, "/target/classes")
    //    val destFeatureDir = new File(project.getBasedir, "target/features")

    getLog.debug(s"Project: $project")
    getLog.debug(s"Project repositories: ${project.getRepositories}")
    getLog.debug(s"Project properties: ${project.getProperties}")
    getLog.debug(s"Project building request: ${project.getProjectBuildingRequest}")
    getLog.debug(s"Project local repository: ${project.getProjectBuildingRequest.getLocalRepository}")

    val localRepoUrl = Option(localRepositoryUrl).getOrElse(project.getProjectBuildingRequest.getLocalRepository.getUrl)
    val remoteRepoUrls = project.getRepositories.asScala.map(r => r.getUrl)

    val features = Option(srcFeatureDir.listFiles()).getOrElse(Array()).filter(f => f.getName.endsWith(featureFileSuffix))
    if (features.isEmpty) throw new BuildFailureException(s"No feature files found in dir: $srcFeatureDir")
    getLog.debug(s"About to process feature files: ${features.map(_.getName).mkString(", ")}")

    val targetFeatureFiles = features.map { featureFile =>
      val targetFile = new File(destFeatureDir, featureFile.getName())
      println(s"Processing feature: $featureFile")

      val repoArgs = if (resolveFromDependencies) {
        project.getArtifacts.asScala.toArray.flatMap { a =>
          Array(
            "--maven-artifact",
            s"${a.getGroupId}:${a.getArtifactId}:${Option(a.getClassifier).filter(_ != "jar").getOrElse("")}:${a.getVersion}:${Option(a.getType).getOrElse("")}",
            a.getFile.getAbsolutePath
          )
        }
      } else {
        Array("--maven-dir", localRepoUrl) ++ remoteRepoUrls.toArray.flatMap(u => Array("--maven-dir", u))
      }

      val debugArgs = if (debug) Array("--debug") else Array[String]()

      val args = Array(
        "-f", featureFile.getAbsolutePath(),
        "-o", targetFile.getAbsolutePath(),
        "--work-dir", new File("target/downloads").getAbsolutePath(),
        "--discard-invalid",
        "--download-missing",
        "--update-checksums"
      ) ++ debugArgs ++ repoArgs

      println(s"Invoking FeatureBuilder with args: ${args.mkString(" ")}")

      FeatureBuilder.run(args)
      targetFile
    }

    getLog.info(s"Produced: ${targetFeatureFiles.mkString(", ")}")

    if (attach) {
      targetFeatureFiles.foreach { featureFile =>
        val name = featureFile.getName
        val classifier = name.substring(0, name.length - featureFileSuffix.length)
        getLog.info(s"Attaching as artifact: $classifier")

        val handler = new DefaultArtifactHandler("conf")
        // val artifact = new DefaultArtifact(project.getGroupId, project.getArtifactId, VersionRange.createFromVersion(project.getVersion), "compile", "conf", classifier, handler)
        val artifact = new AttachedArtifact(project.getArtifact(), attachType, classifier, handler)
        artifact.setFile(featureFile)
        artifact.setResolved(true)
        project.addAttachedArtifact(artifact)
      }
    }
  }

}