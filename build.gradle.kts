import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "2.7.13"
  id("io.spring.dependency-management") version "1.0.15.RELEASE"
  val kotlinVersion = "1.8.22"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.spring") version kotlinVersion
  kotlin("plugin.jpa") version kotlinVersion
}

group = "com.ekino.example"
version = "0.0.1-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_11

java {
  sourceCompatibility = javaVersion
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.flywaydb:flyway-core")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
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
  }
}
