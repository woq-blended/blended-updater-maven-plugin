import org.sonatype.maven.polyglot.scala.model._

import scala.collection.immutable.Seq

implicit val scalaVersion = ScalaVersion("2.12.8")

val pluginVersion = "3.0.13"
val blendedVersion = "3.0.13"

object Deps {
  val mavenVersion = "3.0.5"

  val blendedUpdaterMavenPlugin = "de.wayofquality.blended" % "blended-updater-maven-plugin" % pluginVersion

  val blendedUpdaterTools = "de.wayofquality.blended" %% "blended.updater.tools" % blendedVersion
  val mavenPluginApi = "org.apache.maven" % "maven-plugin-api" % mavenVersion
  val mavenPluginAnnotations = "org.apache.maven.plugin-tools" % "maven-plugin-annotations" % "3.4"
  val mavenCore = "org.apache.maven" % "maven-core" % mavenVersion
  val scalaLibrary = "org.scala-lang" % "scala-library" % scalaVersion.version

  val pluginLifecycle = "org.eclipse.m2e" % "lifecycle-mapping" % "1.0.0"
  val pluginPlugin = "org.apache.maven.plugins" % "maven-plugin-plugin" % "3.5.2"
  val pluginScala = "net.alchim31.maven" % "scala-maven-plugin" % "3.3.2"
}

Model(
  gav = Deps.blendedUpdaterMavenPlugin,
  packaging = "maven-plugin",
  description = "Integration of Blended Updater feature / product builds into Maven",
  prerequisites = Prerequisites(
    maven = s"${Deps.mavenVersion}"
  ),
  dependencies = Seq(
    Deps.mavenPluginAnnotations % "provided",
    Deps.mavenPluginApi,
    Deps.mavenCore,
    Deps.blendedUpdaterTools,
    Deps.scalaLibrary
  ),
  build = Build(
    pluginManagement = PluginManagement(
      plugins = Seq(
        // ignore maven-plugin-plugin in Eclipse
        Plugin(
          gav = Deps.pluginLifecycle,
          configuration = Config(
            pluginExection = Config(
              pluginExecutionFilter = Config(
                groupId = Deps.pluginPlugin.groupId.get,
                artifactId = Deps.pluginPlugin.artifactId,
                versionRange = "[0,)",
                goals = Config(
                  goal = "descriptor",
                  goal = "help-goal"
                )
              ),
              action = Config(
                ignore = None
              )
            )
          )
        )
      )
    ),
    plugins = Seq(
      Plugin(
        gav = Deps.pluginScala,
        executions = Seq(
          Execution(
            goals = Seq("add-source", "compile", "testCompile")
          )
        ),
        configuration = Config(
          scalaVersion = scalaVersion.version,
          fork = "true",
          recompileMode = "incremental",
          useZincServer = "true",
          args = Config(
            arg = "-deprecation",
            arg = "-feature",
            arg = "-Xlint",
            arg = "-Ywarn-nullary-override"
          )
        )
      ),
      Plugin(
        gav = Deps.pluginPlugin,
        executions = Seq(
          Execution(
            id = "default-descriptor",
            phase = "process-classes",
            goals = Seq(
              "descriptor"
            )
          ),
          Execution(
            id = "help-goal",
            goals = Seq(
              "helpmojo"
            ),
            configuration = Config(
              skipErrorNoDescriptorsFound = "true"
            )
          )
        ),
        configuration = Config(
          goalPrefix = "blended-updater"
        )
      )
    )
  )
)
