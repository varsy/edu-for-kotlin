import groovy.util.Node
import groovy.xml.XmlParser
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformTestingExtension
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val environmentName: String by project
// BACKCOMPAT: 2024.2. Drop it, it's always true
val isAtLeast243: Boolean = environmentName.toInt() >= 243

val pluginVersion: String by project
val platformVersion: String = "20${StringBuilder(environmentName).insert(environmentName.length - 1, '.')}"
val baseIDE: String by project
val isJvmCenteredIDE = baseIDE in listOf("idea", "studio")

val ideaVersion: String by project

val clionVersion: String by project
val pycharmVersion: String by project
val studioVersion: String by project
val riderVersion: String by project

val isIdeaIDE = baseIDE == "idea"
val isClionIDE = baseIDE == "clion"
val isPycharmIDE = baseIDE == "pycharm"
val isStudioIDE = baseIDE == "studio"
val isRiderIDE = baseIDE == "rider"

val baseVersion = when {
  isIdeaIDE -> ideaVersion
  isClionIDE -> clionVersion
  isPycharmIDE -> pycharmVersion
  isStudioIDE -> studioVersion
  isRiderIDE -> riderVersion
  else -> error("Unexpected IDE name = `$baseIDE`")
}

val pythonProPlugin: String by project
val pythonCommunityPlugin: String by project

val pythonPlugin = when {
  // Since 2024.2 Python Community plugin is available in paid products (like IU) together with Python Pro as its base dependency.
  // But all necessary code that we need is inside Python Community plugin, so we need only it from compilation POV
  isIdeaIDE -> pythonCommunityPlugin
  isClionIDE -> "PythonCore"
  isPycharmIDE -> "PythonCore"
  isStudioIDE -> pythonCommunityPlugin
  isRiderIDE -> pythonCommunityPlugin
  else -> error("Unexpected IDE name = `$baseIDE`")
}
val javaPlugin = "com.intellij.java"
val kotlinPlugin = "org.jetbrains.kotlin"
val scalaPlugin: String by project
val rustPlugin: String by project
val tomlPlugin = "org.toml.lang"
val goPlugin: String by project
val sqlPlugin = "com.intellij.database"
val shellScriptPlugin = "com.jetbrains.sh"
val markdownPlugin = "org.intellij.plugins.markdown"
val githubPlugin = "org.jetbrains.plugins.github"
val psiViewerPlugin: String by project
val phpPlugin: String by project
val intelliLangPlugin = "org.intellij.intelliLang"
val javaScriptPlugin = "JavaScript"
val nodeJsPlugin = "NodeJS"
val jsonPlugin = "com.intellij.modules.json"
val yamlPlugin = "org.jetbrains.plugins.yaml"
val androidPlugin = "org.jetbrains.android"
val codeWithMePlugin = "com.jetbrains.codeWithMe"


val jvmPlugins = listOf(
  javaPlugin,
  "JUnit",
  "org.jetbrains.plugins.gradle"
)

val javaScriptPlugins = listOf(
  javaScriptPlugin,
  nodeJsPlugin
)

val rustPlugins = listOf(
  rustPlugin,
  tomlPlugin
)

val cppPlugins = listOf(
  "com.intellij.clion",
  "com.intellij.clion.runFile",
  "com.intellij.nativeDebug",
  "org.jetbrains.plugins.clion.test.google",
  "org.jetbrains.plugins.clion.test.catch"
)

val sqlPlugins = listOf(
  sqlPlugin,
  // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1791
  "intellij.charts"
)

val csharpPlugins = listOf(
  "com.intellij.resharper.unity"
)

val ideToPlugins = mapOf(
  // Since 2024.2 Python Community plugin is available in paid products (like IU) together with Python Pro as its base dependency.
  // Actually, Python Community contains all necessary code that we need.
  // Python Pro plugin is added here on 2024.2 just to have the most common setup from user POV (i.e. Python Community + Python Pro)
  IntellijIdeaUltimate to listOfNotNull(scalaPlugin, rustPlugin, pythonProPlugin, pythonCommunityPlugin.takeIf { true }, goPlugin, phpPlugin),
  CLion to listOf(rustPlugin),
  AndroidStudio to listOf(pythonCommunityPlugin),
  GoLand to listOf(pythonCommunityPlugin),
  RustRover to listOf(pythonCommunityPlugin)
)

fun idePlugins(type: IntelliJPlatformType): List<String> {
  return ideToPlugins[type].orEmpty() + psiViewerPlugin
}

/**
 * Modules created automatically because of the project file structure.
 * These modules are not intended to be part of plugin,
 * so they shouldn't be set up in the same way as others.
 *
 * Mostly workaround to overcome cons of using `allprojects` and `subprojects`
 */
val implicitModules = setOf(":intellij-plugin:features")

allprojects {
  if (path in implicitModules) return@allprojects

  apply {
    plugin("org.jetbrains.kotlin.plugin.serialization")
  }

  sourceSets {
    main {
      java.srcDirs("src", "branches/$environmentName/src")
      resources.srcDirs("resources", "branches/$environmentName/resources")
    }

    test {
      java.srcDirs("testSrc", "branches/$environmentName/testSrc")
      resources.srcDirs("testResources", "branches/$environmentName/testResources")
    }
  }

  kotlin {
    sourceSets {
      main {
        kotlin.srcDirs("src", "branches/$environmentName/src")
      }
      test {
        kotlin.srcDirs("testSrc", "branches/$environmentName/testSrc")
      }
    }
  }

  tasks {
    withType<JavaCompile> {
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }
    withType<KotlinCompile> {
      // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
      inputs.property("environmentName", providers.gradleProperty("environmentName"))
    }

    val verifyClasses = task("verifyClasses") {
      dependsOn(jar)
      doLast {
        verifyClasses(project)
      }
    }
    // Fail plugin build if there are errors in module packages
    project(":intellij-plugin").tasks.buildPlugin {
      dependsOn(verifyClasses)
    }
  }

  dependencies {
    implementationWithoutKotlin(rootProject.libs.twitter4j.core)
    implementationWithoutKotlin(rootProject.libs.twitter4j.v2)
    implementationWithoutKotlin(rootProject.libs.jsoup)
    implementationWithoutKotlin(rootProject.libs.jackson.dataformat.yaml)
    implementationWithoutKotlin(rootProject.libs.jackson.datatype.jsr310)
    implementationWithoutKotlin(rootProject.libs.jackson.module.kotlin)
    implementationWithoutKotlin(rootProject.libs.okhttp)
    implementationWithoutKotlin(rootProject.libs.logging.interceptor)
    implementationWithoutKotlin(rootProject.libs.retrofit)
    implementationWithoutKotlin(rootProject.libs.converter.jackson)
    implementationWithoutKotlin(rootProject.libs.kotlin.css.jvm)

    testImplementation(rootProject.libs.junit)
    testImplementation(rootProject.libs.openTest4J)
    testImplementation(rootProject.libs.classgraph)
    testImplementationWithoutKotlin(rootProject.libs.kotlin.test.junit)
    testImplementationWithoutKotlin(rootProject.libs.mockwebserver)
    testImplementationWithoutKotlin(rootProject.libs.mockk)
  }
}

subprojects {
  if (path in implicitModules) return@subprojects

  apply {
    plugin("org.jetbrains.intellij.platform.module")
  }

  repositories {
    intellijPlatform {
      defaultRepositories()
      jetbrainsRuntime()
    }
  }

  intellijPlatform {
    instrumentCode = false
  }

  tasks {
    prepareSandbox { enabled = false }
  }

  val testOutput = configurations.create("testOutput")

  dependencies {
    testOutput(sourceSets.test.get().output.classesDirs)

    intellijPlatform {
      testFramework(TestFrameworkType.Bundled)
    }
  }
}

plugins {
  alias(libs.plugins.intelliJPlatformPlugin)
}

repositories {
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

val buildNumber = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"

if (hasProp("setTCBuildNumber")) {
  // Specify build number at building plugin running configuration on TC
  // with heading plugin version: e.g. `3.8.BUILD_NUMBER` instead of `BUILD_NUMBER`
  println("##teamcity[buildNumber '$pluginVersion.$buildNumber']")
}

version = "$pluginVersion-$platformVersion-$buildNumber"

intellijPlatform {
  projectName = "JetBrainsAcademy"
  pluginConfiguration {
    id = "com.jetbrains.edu"
    name = "JetBrains Academy"
    version = "$pluginVersion-$platformVersion-$buildNumber"
    changeNotes = provider { file("changes.html").readText() }
    description = provider { file("description.html").readText() }

    ideaVersion {
      sinceBuild = prop("customSinceBuild")
      untilBuild = prop("customUntilBuild")
    }

    vendor {
      name = "JetBrains"
    }
  }
  instrumentCode = false
  buildSearchableOptions = prop("enableBuildSearchableOptions").toBoolean()
}

dependencies {
  intellijPlatform {
    intellijIde(project, baseVersion)

    pluginModule(implementation(project("educational-core")))
    pluginModule(implementation(project("code-insight")))
    pluginModule(implementation(project("code-insight:html")))
    pluginModule(implementation(project("code-insight:markdown")))
    pluginModule(implementation(project("code-insight:yaml")))
    pluginModule(implementation(project("jvm-core")))
    pluginModule(implementation(project("AI")))
    pluginModule(implementation(project("Edu-Java")))
    pluginModule(implementation(project("Edu-Kotlin")))
    pluginModule(implementation(project("Edu-Python")))
    pluginModule(implementation(project("Edu-Python:Idea")))
    pluginModule(implementation(project("Edu-Python:PyCharm")))
    pluginModule(implementation(project("Edu-Scala")))
    pluginModule(implementation(project("Edu-Android")))
    pluginModule(implementation(project("Edu-JavaScript")))
    pluginModule(implementation(project("Edu-Rust")))
    pluginModule(implementation(project("Edu-Cpp")))
    pluginModule(implementation(project("Edu-Cpp:CLion-Classic")))
    pluginModule(implementation(project("Edu-Cpp:CLion-Nova")))
    pluginModule(implementation(project("Edu-Go")))
    pluginModule(implementation(project("Edu-Php")))
    pluginModule(implementation(project("Edu-Shell")))
    pluginModule(implementation(project("Edu-CSharp")))
    pluginModule(implementation(project("sql")))
    pluginModule(implementation(project("sql:sql-jvm")))
    pluginModule(implementation(project("github")))
    pluginModule(implementation(project("remote-env")))
    pluginModule(implementation(project("features:command-line")))
    pluginModule(implementation(project("features:ai-hints-core")))
    pluginModule(implementation(project("features:ai-hints-kotlin")))
    pluginModule(implementation(project("features:ai-hints-python")))
    pluginModule(implementation(project("localization")))
    pluginModule(implementation(project("Edu-Jarvis")))

    testFramework(TestFrameworkType.Bundled)
  }
}

tasks {
  val projectName = project.extensionProvider.flatMap { it.projectName }

  composedJar {
    archiveBaseName.convention(projectName)
  }

  withType<PrepareSandboxTask> {
    from("socialMedia") {
      into("${projectName.get()}/socialMedia")
      include("**/*.gif")
    }
    doLast {
      val kotlinJarRe = """kotlin-(stdlib|reflect|runtime).*\.jar""".toRegex()
      val libraryDir = destinationDir.resolve("${projectName.get()}/lib")
      val kotlinStdlibJars = libraryDir.listFiles().orEmpty().filter { kotlinJarRe.matches(it.name) }
      check(kotlinStdlibJars.isEmpty()) {
        "Plugin shouldn't contain kotlin stdlib jars. Found:\n" + kotlinStdlibJars.joinToString(separator = ",\n") { it.absolutePath }
      }
    }
  }
  withType<RunIdeTask> {
    // Disable auto plugin reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
    // To enable dynamic reloading, change value to `true` and disable `EduDynamicPluginListener`
    autoReload = false
    jvmArgs("-Xmx2g")
    jvmArgs("-Dide.experimental.ui=true")

    // Uncomment to show localized messages
    // jvmArgs("-Didea.l10n=true")

    // Uncomment to enable memory dump creation if plugin cannot be unloaded by the platform
    // jvmArgs("-Dide.plugins.snapshot.on.unload.fail=true")

    // Uncomment to enable FUS testing mode
    // jvmArgs("-Dfus.internal.test.mode=true")
  }
  buildPlugin {
    dependsOn(":edu-format:jar")
    dependsOn(":edu-format:sourcesJar")
    doLast {
      copyFormatJars()
    }
  }

  intellijPlatformTesting {
    // Generates event scheme for JetBrains Academy plugin FUS events to `build/eventScheme.json`
    runIde.register("buildEventsScheme") {
      task {
        args("buildEventsScheme", "--outputFile=${buildDir()}/eventScheme.json", "--pluginId=com.jetbrains.edu")
        // Force headless mode to be able to run command on CI
        systemProperty("java.awt.headless", "true")
        // BACKCOMPAT: 2024.2. Update value to 243 and this comment
        // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
        // It will be used by TeamCity automation to set minimal IDE version for new events
        environment("IDEA_BUILD_NUMBER", "242")
      }
    }

    runIde.register("runInSplitMode") {
      splitMode = true

      // Specify custom sandbox directory to have a stable path to log file
      sandboxDirectory = intellijPlatform.sandboxContainer.dir("split-mode-sandbox-$environmentName")

      plugins {
        val type = baseVersion.toTypeWithVersion().type
        plugins(idePlugins(type))
      }
    }

    customRunIdeTask(IntellijIdeaUltimate, ideaVersion, baseTaskName = "Idea")
    customRunIdeTask(CLion, clionVersion) {
      setClionSystemProperties(withRadler = false)
    }
    customRunIdeTask(CLion, clionVersion, baseTaskName = "CLion-Nova") {
      setClionSystemProperties(withRadler = true)
    }
    customRunIdeTask(PyCharmCommunity, pycharmVersion, baseTaskName = "PyCharm")
    customRunIdeTask(AndroidStudio, studioVersion)
    customRunIdeTask(WebStorm)
    customRunIdeTask(GoLand)
    customRunIdeTask(PhpStorm)
    customRunIdeTask(RustRover)
    customRunIdeTask(DataSpell)
    customRunIdeTask(Rider, riderVersion)
  }
}

/**
 * Creates `run$[baseTaskName]` Gradle task to run IDE of given [type]
 * via `runIde` task with plugins according to [ideToPlugins] map
 */
fun IntelliJPlatformTestingExtension.customRunIdeTask(
  type: IntelliJPlatformType,
  versionWithCode: String? = null,
  baseTaskName: String = type.name,
  configureRunIdeTask: RunIdeTask.() -> Unit = {},
) {
  runIde.register("run$baseTaskName") {
    useInstaller = false

    if (versionWithCode != null) {
      val version = versionWithCode.toTypeWithVersion().version

      this.type = type
      this.version = version
    }
    else {
      val pathProperty = baseTaskName.replaceFirstChar { it.lowercaseChar() } + "Path"
      // Avoid throwing exception during property calculation.
      // Some IDE tooling (for example, Package Search plugin) may try to calculate properties during `Sync` phase for all tasks.
      // In our case, some `run*` task may not have `pathProperty` in your `gradle.properties`,
      // and as a result, the `Sync` tool window will show you the error thrown by `prop` function.
      //
      // The following solution just moves throwing the corresponding error to task execution,
      // i.e., only when a task is actually invoked
      if (hasProp(pathProperty)) {
        localPath.convention(layout.dir(provider { file(prop(pathProperty)) }))
      }
      else {
        task {
          doFirst {
            throw GradleException("Property `$pathProperty` is not defined in gradle.properties")
          }
        }
      }
    }

    // Specify custom sandbox directory to have a stable path to log file
    sandboxDirectory = intellijPlatform.sandboxContainer.dir("${baseTaskName.lowercase()}-sandbox-$environmentName")

    task(configureRunIdeTask)

    plugins {
      plugins(idePlugins(type))
    }
  }
}

project("educational-core") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)

      bundledModules("intellij.platform.vcs.impl")
    }

    implementation("org.jetbrains.academy.jarvis.dsl:Jarvis-no-code-in-edu:1.0.0") {
      excludeKotlinDeps()
    }
    api(project(":edu-format"))
    api(rootProject.libs.edu.ai.format) {
      excludeKotlinDeps()
    }
    // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
    // and fails Kotlin compilation.
    // Let's provide necessary dependency during compilation to make it work
    compileOnly(rootProject.libs.kotlinx.serialization) {
      excludeKotlinDeps()
    }
  }
}

project("code-insight") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)
    }

    implementation(project(":intellij-plugin:educational-core"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("code-insight:html") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("code-insight:markdown") {
  intellijPlatform {
    // Set custom plugin directory name for tests.
    // Otherwise, `prepareTestSandbox` merges directories of `markdown` plugin and `markdown` modules
    // into single one
    projectName = "edu-markdown"
  }

  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)

      intellijPlugins(markdownPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("code-insight:yaml") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)

      intellijPlugins(yamlPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:code-insight"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
  }
}

project("jvm-core") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      intellijPlugins(jvmPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("remote-env") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (isStudioIDE || isRiderIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      intellijPlugins(codeWithMePlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("AI") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)
    }

    implementation(project(":intellij-plugin:educational-core"))

    compileOnly(rootProject.libs.kotlinx.serialization) {
      excludeKotlinDeps()
    }

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Java") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(jvmPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("Edu-Kotlin") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion

      intellijIde(project, ideVersion)

      intellijPlugins(jvmPlugins)
      intellijPlugins(kotlinPlugin)
    }

    tasks.test {
      jvmArgumentProviders += CommandLineArgumentProvider {
        // Force turning Kotlin V2 off in IDE, otherwise our Kotlin module does not load. Remove after EDU-7532
        listOf("-Didea.kotlin.plugin.use.k2=false")
      }
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))

    implementation(project(":intellij-plugin:Edu-Jarvis"))
    testImplementation(project(":intellij-plugin:Edu-Jarvis", "testOutput"))
  }
}

project("Edu-Scala") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(jvmPlugins)
      intellijPlugins(scalaPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("Edu-Android") {
  dependencies {
    intellijPlatform {
      intellijIde(project, studioVersion)

      intellijPlugins(jvmPlugins)
      // TODO: make `kotlinPlugin` test-only
      intellijPlugins(androidPlugin, kotlinPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }

  // BACKCOMPAT: enable when 243 studio is available
  tasks.withType<Test> {
    enabled = environmentName.toInt() < 243
  }
}

project("Edu-Python") {
  dependencies {
    intellijPlatform {
      // needed to load `org.toml.lang plugin` for Python plugin in tests
      val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      val pluginList = listOfNotNull(
        pythonPlugin,
        if (isJvmCenteredIDE) javaPlugin else null,
        // needed to load `intellij.python.community.impl` module of Python plugin in tests
        tomlPlugin
      )
      intellijPlugins(pluginList)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:Edu-Python:Idea"))
    testImplementation(project(":intellij-plugin:Edu-Python:PyCharm"))
  }
}

project("Edu-Python:Idea") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      val pluginList = listOf(
        if (!isJvmCenteredIDE) pythonCommunityPlugin else pythonPlugin,
        javaPlugin
      )
      intellijPlugins(pluginList)
    }

    implementation(project(":intellij-plugin:educational-core"))
    compileOnly(project(":intellij-plugin:Edu-Python"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Python:PyCharm") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (isStudioIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      // TODO: incorrect plugin version in case of AS
      intellijPlugins(pythonPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    compileOnly(project(":intellij-plugin:Edu-Python"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-JavaScript") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(javaScriptPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Rust") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (!isIdeaIDE && !isClionIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      intellijPlugins(rustPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }

  // ATM all tests fail because they can't find test implementation of `CargoProjectsService`.
  // It was moved to test sources recently in the Rust plugin project, but plugin manifest declares separate test impl of the service.
  // As a result, tests fail.
  //
  // Enable test again when the corresponding problem is fixed
  tasks.withType<Test> {
    enabled = false
  }
}

project("Edu-Cpp") {
  tasks {
    test {
      setClionSystemProperties(withRadler = false)
    }
  }

  dependencies {
    intellijPlatform {
      intellijIde(project, clionVersion)

      intellijPlugins(cppPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Cpp:CLion-Classic") {
  dependencies {
    intellijPlatform {
      intellijIde(project, clionVersion)

      intellijPlugins(cppPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:Edu-Cpp"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Cpp:CLion-Nova") {
  dependencies {
    intellijPlatform {
      intellijIde(project, clionVersion)

      intellijPlugins(cppPlugins)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:Edu-Cpp"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Go") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(goPlugin, intelliLangPlugin)
      if (isAtLeast243) {
        intellijPlugins(jsonPlugin)
      }
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Php") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(phpPlugin)
      if (isAtLeast243) {
        intellijPlugins(jsonPlugin)
      }
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-Shell") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)

      intellijPlugins(shellScriptPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("Edu-CSharp") {
  dependencies {
    intellijPlatform {
      intellijIde(project, riderVersion)
      intellijPlugins(csharpPlugins)

      bundledModule("intellij.rider")
    }

    implementation(project(":intellij-plugin:educational-core"))
    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("sql") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (isStudioIDE || isPycharmIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      intellijPlugins(sqlPlugins)
    }

    api(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("sql:sql-jvm") {
  dependencies {
    intellijPlatform {
      intellijIde(project, ideaVersion)

      intellijPlugins(jvmPlugins)
      intellijPlugins(sqlPlugins)
    }

    api(project(":intellij-plugin:sql"))
    api(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:sql", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  }
}

project("github") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)

      intellijPlugins(githubPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("localization") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)
    }
  }
}

project("features:command-line") {
  dependencies {
    intellijPlatform {
      // TODO: use `baseVersion` when https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1790 is resolved
      intellijIde(project, ideaVersion)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementationWithoutKotlin(rootProject.libs.clikt.core)

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("features:ai-hints-core") {
  dependencies {
    intellijPlatform {
      intellijIde(project, baseVersion)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:AI"))
    api(rootProject.libs.educational.ml.library.core) {
      excludeKotlinDeps()
      excludeKotlinSerializationDeps()
      exclude(group = "net.java.dev.jna")
    }
    api(rootProject.libs.educational.ml.library.hints) {
      excludeKotlinDeps()
      excludeKotlinSerializationDeps()
      exclude(group = "net.java.dev.jna")
    }
    // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
    // and fails Kotlin compilation.
    // Let's provide necessary dependency during compilation to make it work
    compileOnly(rootProject.libs.kotlinx.serialization) {
      excludeKotlinDeps()
    }

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  }
}

project("features:ai-hints-kotlin") {
  dependencies {
    intellijPlatform {
      val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      intellijPlugins(kotlinPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:features:ai-hints-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:features:ai-hints-core", "testOutput"))
  }
}

project("features:ai-hints-python") {
  dependencies {
    intellijPlatform {
      // needed to load `org.toml.lang plugin` for Python plugin in tests
      val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
      intellijIde(project, ideVersion)

      val pluginList = listOfNotNull(
        pythonPlugin,
        if (isJvmCenteredIDE) javaPlugin else null,
        // needed to load `intellij.python.community.impl` module of Python plugin in tests
        tomlPlugin
      )
      intellijPlugins(pluginList)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:features:ai-hints-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:features:ai-hints-core", "testOutput"))
    testImplementation(project(":intellij-plugin:Edu-Python"))
  }
}

project("Edu-Jarvis") {
  dependencies {
    intellijPlatform {
      // Kotlin plugin cannot be found in 242 builds because of https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1652,
      // and as a result, it's impossible to build the module with Kotlin plugin dependency.
      // As a temporary workaround, let's build the module with old IDE version.
      // Should be fixed as part of https://youtrack.jetbrains.com/issue/EDU-6934
      val ideVersion = if (environmentName.toInt() == 242) {
        "IU-2024.1"
      }
      else {
        if (!isJvmCenteredIDE) ideaVersion else baseVersion
      }

      intellijIde(project, ideVersion)

      intellijPlugins(jvmPlugins)
      intellijPlugins(kotlinPlugin)
    }

    implementation(project(":intellij-plugin:educational-core"))
    implementation(project(":intellij-plugin:jvm-core"))

    testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
    testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))

    implementation("org.jetbrains.academy.jarvis.dsl:Jarvis-no-code-in-edu:1.0.0") {
      excludeKotlinDeps()
    }
  }
}

data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)

fun String.toTypeWithVersion(): TypeWithVersion {
  val (code, version) = split("-", limit = 2)
  return TypeWithVersion(IntelliJPlatformType.fromCode(code), version)
}

fun IntelliJPlatformDependenciesExtension.intellijIde(project: Project, versionWithCode: String) {
  val (type, version) = versionWithCode.toTypeWithVersion()
  create(type, version, useInstaller = false)

  // Workaround for https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1738
  // It will become redundant since IJPGP 2.2.0
  if (type == AndroidStudio) {
    project.configurations {
      intellijPlatformBundledModules {
        exclude(Configurations.Dependencies.BUNDLED_MODULE_GROUP, "com.jetbrains.performancePlugin")
      }
    }
  }

  // JetBrains runtime is necessary not only for running IDE but for tests as well
  if (hasProp("jbrVersion")) {
    jetbrainsRuntime(prop("jbrVersion"))
  }
  else {
    jetbrainsRuntime()
  }
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(vararg notations: String) {
  for (notation in notations) {
    if (notation.contains(":")) {
      plugin(notation)
    }
    else {
      bundledPlugin(notation)
    }
  }
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(notations: List<String>) {
  intellijPlugins(*notations.toTypedArray())
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String =
  extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")

fun buildDir(): String {
  return project.layout.buildDirectory.get().asFile.absolutePath
}

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  // Kotlin stdlib
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
  exclude(module = "kotlin-stdlib-jdk7")
  // Kotlin coroutines
  exclude(module = "kotlinx-coroutines-core")
  exclude(module = "kotlinx-coroutines-core-jvm")
  exclude(module = "kotlinx-coroutines-jdk8")
  exclude(module = "kotlinx-coroutines-slf4j")
}

fun <T : ModuleDependency> T.excludeKotlinSerializationDeps() {
  // Kotlin serialization
  exclude(module = "kotlinx-serialization-core-jvm")
  exclude(module = "kotlinx-serialization-json")
}

fun parseManifest(file: File): Node {
  val node = XmlParser().parse(file)
  check(node.name() == "idea-plugin") {
    "Manifest file `$file` doesn't contain top-level `idea-plugin` attribute"
  }
  return node
}

fun manifestFile(project: Project): File? {
  var filePath: String? = null
  // Some gradle projects are not modules from IDEA plugin point of view
  // because we use `include` for them inside manifests, i.e. they just a part of another module.
  // That's why we delegate manifest search to other projects in some cases
  when (project.path) {
    ":intellij-plugin" -> {
      filePath = "META-INF/plugin.xml"
    }
    ":intellij-plugin:educational-core", ":intellij-plugin:code-insight",
    ":intellij-plugin:Edu-Python:Idea", ":intellij-plugin:Edu-Python:PyCharm" -> return manifestFile(project.parent!!)
    // Localization module is not supposed to have a plugin manifest.
    // Since it also is not supposed to have any code, only resources, no need to verify anything for it
    ":intellij-plugin:localization" -> return null
  }

  val mainOutput = project.sourceSets.main.get().output
  val resourcesDir = mainOutput.resourcesDir ?: error("Failed to find resources dir for ${project.name}")

  if (filePath != null) {
    return resourcesDir.resolve(filePath).takeIf { it.exists() } ?: error("Failed to find manifest file for ${project.name} module")
  }
  val rootManifestFile = manifestFile(project(":intellij-plugin")) ?: error("Failed to find manifest file for :intellij-plugin module")
  val rootManifest = parseManifest(rootManifestFile)
  val children = ((rootManifest["content"] as? List<*>)?.single() as? Node)?.children()
                 ?: error("Failed to find module declarations in root manifest")
  return children.filterIsInstance<Node>()
           .flatMap { node ->
             if (node.name() != "module") return@flatMap emptyList()
             val name = node.attribute("name") as? String ?: return@flatMap emptyList()
             listOfNotNull(resourcesDir.resolve("$name.xml").takeIf { it.exists() })
           }.firstOrNull() ?: error("Failed to find manifest file for ${project.name} module")
}

fun findModulePackage(project: Project): String? {
  val moduleManifest = manifestFile(project) ?: return null
  val node = parseManifest(moduleManifest)
  return node.attribute("package") as? String ?: error("Failed to find package for ${project.name}")
}

fun verifyClasses(project: Project) {
  val pkg = findModulePackage(project) ?: return
  val expectedDir = pkg.replace('.', '/')

  var hasErrors = false
  for (classesDir in project.sourceSets.main.get().output.classesDirs) {
    val basePath = classesDir.toPath()
    for (file in classesDir.walk()) {
      if (file.isFile && file.extension == "class") {
        val relativePath = basePath.relativize(file.toPath())
        if (!relativePath.startsWith(expectedDir)) {
          logger.error("Wrong package of `${relativePath.joinToString(".").removeSuffix(".class")}` class. Expected `$pkg`")
          hasErrors = true
        }
      }
    }
  }

  if (hasErrors) {
    throw GradleException("Classes with wrong package were found. See https://docs.google.com/document/d/1pOy-qNlGOJe6wftHVYHkH8sZOoAfav1fdGDPJgkQWJo")
  }
}

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: Provider<*>) {
  implementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun DependencyHandler.testImplementationWithoutKotlin(dependencyNotation: Provider<*>) {
  testImplementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun copyFormatJars() {
  copy {
    from("../edu-format/build/libs/")
    into("build/distributions")
    include("*.jar")
  }
}

// Since 2024.1 CLion has two sets of incompatible plugins: based on classic language engine and new one (AKA Radler).
// Platform uses `idea.suppressed.plugins.set.selector` system property to choose which plugins should be disabled.
// But there aren't `idea.suppressed.plugins.set.selector`, `idea.suppressed.plugins.set.classic`
// and `idea.suppressed.plugins.set.radler` properties in tests,
// as a result, the platform tries to load all plugins and fails because of duplicate definitions.
// Here is a workaround to make test work with CLion by defining proper values for necessary properties
fun JavaForkOptions.setClionSystemProperties(withRadler: Boolean = false) {
  val (mode, suppressedPlugins) = if (withRadler) {
    val radlerSuppressedPlugins = listOfNotNull(
      "com.intellij.cidr.lang",
      "com.intellij.cidr.lang.clangdBridge",
      "com.intellij.c.performanceTesting",
      "org.jetbrains.plugins.cidr-intelliLang",
      "com.intellij.cidr.grazie",
      "com.intellij.cidr.markdown",
    )
    "radler" to radlerSuppressedPlugins
  }
  else {
    val classicSuppressedPlugins = listOf(
      "org.jetbrains.plugins.clion.radler",
      "intellij.rider.cpp.debugger",
      "intellij.rider.plugins.clion.radler.cwm"
    )
    "classic" to classicSuppressedPlugins
  }
  systemProperty("idea.suppressed.plugins.set.selector", mode) // possible values: `classic` and `radler`
  systemProperty("idea.suppressed.plugins.set.$mode", suppressedPlugins.joinToString(","))
}
