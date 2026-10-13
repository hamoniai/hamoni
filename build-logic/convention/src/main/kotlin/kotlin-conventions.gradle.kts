import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Conventions for Kotlin projects
 */
plugins {
    id("java-conventions")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")
}

/**
 * Dependencies for use wth Kotlin
 */
dependencies {
    val kotlinSerializationBom = libs.findLibrary("kotlin.serialization.bom").get()
    implementation(enforcedPlatform(kotlinSerializationBom))
    testImplementation(enforcedPlatform(kotlinSerializationBom))
}

/**
 * Standard Kotlin configuration
 */
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    compilerOptions {
        javaParameters = true
    }
}

/**
 * Generate the pom.xml and pom.properties before the Kotlin compilation so that the annotation processors have
 * access to the POM.
 */
tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.named { it.startsWith("generatePomFileFor") || it == "generatePomPropertiesFile"})
}

/**
 * KAPT configuration. Need to keep the standard processors due to JBoss Logging.
 */
kapt {
    keepJavacAnnotationProcessors = true
    includeCompileClasspath = false
}

/**
 * Disable kapt at test time as it is not required
 */
tasks.named { it == "kaptGenerateStubsTestKotlin" || it == "kaptTestKotlin" }.configureEach {
    enabled = false
}

/**
 * Configuration for allOpen
 */
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("io.quarkus.runtime.annotations.Recorder")
}

