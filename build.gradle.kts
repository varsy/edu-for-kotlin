import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(from = "common.gradle.kts")

val pluginVersion: String by project
val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

plugins {
  idea
  alias(libs.plugins.kotlinPlugin)
  alias(libs.plugins.kotlinSerializationPlugin)
  alias(libs.plugins.downloadPlugin)
  alias(libs.plugins.propertiesPlugin)
  alias(libs.plugins.testRetryPlugin)
}

idea {
  project {
    jdkName = "17"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
  }
}

allprojects {
  apply {
    plugin("java")
    plugin("kotlin")
    plugin("net.saliman.properties")
    plugin("org.gradle.test-retry")
  }

  repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    maven("https://packages.jetbrains.team/maven/p/edu/jarvis")
    maven("https://packages.jetbrains.team/maven/p/edu/maven")
    maven("https://packages.jetbrains.team/maven/p/edu/educational-ml-library")
    maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = VERSION_17
    targetCompatibility = VERSION_17
  }

  tasks {
    withType<Test> {
      withProp("excludeTests") { exclude(it) }

      ignoreFailures = true
      filter {
        isFailOnNoMatchingTests = false
      }
      if (isTeamCity) {
        retry {
          maxRetries = 3
          maxFailures = 5
        }
      }
    }

    withType<JavaCompile> {
      options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
      compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        languageVersion = KotlinVersion.DEFAULT
        // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
        apiVersion = KotlinVersion.KOTLIN_1_8
        freeCompilerArgs = listOf("-Xjvm-default=all")
      }
    }

    jar {
      // Starting from gradle-intellij-plugin 1.6.0, test runs produces `classpath.index` file in `class` directory
      // But this file shouldn't be included into final module artifact at all, so exclude it manually for now
      exclude("**/classpath.index")
    }
  }
}

// For some reason, `version = "$pluginVersion.0"` inside `fleet-plugin/build.gradle.kts` is not enough.
// It seems fleet gradle plugin reads project version too early when it's not set yet.
// This code executed before `fleet-plugin/build.gradle.kts` is evaluated,
// so at the moment of reading version is already set.
//
// `.0` is needed because fleet plugin should have only `major.minor.patch` version structure
if (prop("fleetIntegration").toBoolean()) {
  project(":fleet-plugin") {
    version = "$pluginVersion.0"
  }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String =
  extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")

fun withProp(name: String, action: (String) -> Unit) {
  if (hasProp(name)) {
    action(prop(name))
  }
}
