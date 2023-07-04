import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Versions are defined in `gradle/libs.versions.toml`
// See https://docs.gradle.org/current/userguide/platforms.html#sub::toml-dependencies-format

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.spring)
  alias(libs.plugins.kotlin.plugin.jpa)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.boot.deps)
}

group = "com.ekino.example"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.logging)
  runtimeOnly(libs.h2)

  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.assertk)
}

allOpen {
  annotation("javax.persistence.Entity")
  annotation("javax.persistence.MappedSuperclass")
  annotation("javax.persistence.Embeddable")
}

val javaVersion = JavaVersion.VERSION_11

java {
  sourceCompatibility = javaVersion
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs += "-Xjsr305=strict"
      jvmTarget = javaVersion.majorVersion
    }
  }

  withType<Test> {
    useJUnitPlatform()
    testLogging {
      events = setOf(
        TestLogEvent.PASSED,
        TestLogEvent.FAILED,
        TestLogEvent.STANDARD_OUT,
      )
    }
  }
}
