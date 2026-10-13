/**
 * Turn a plugin version library reference into a dependency
 */
fun Provider<PluginDependency>.toDependency(): Provider<Dependency> {
    return this.map { project.dependencies.create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }
}

/**
 * Plugins used by the build script
 */
plugins {
    `kotlin-dsl`
}

/**
 * Default repositories
 */
repositories {
    mavenCentral()
    gradlePluginPortal()
}

/**
 * Dependencies
 */
dependencies {
    // Set the versions of plugins used by the build
    implementation(libs.plugins.kotlin.core.toDependency())
    implementation(libs.plugins.kotlin.allopen.toDependency())
    implementation(libs.plugins.kotlin.serialization.toDependency())
    implementation(libs.plugins.platform.quarkus.application.toDependency())
    implementation(libs.plugins.platform.quarkus.extension.toDependency())
    implementation(libs.plugins.util.jandex.toDependency())

    // Support for integrating with AWS directly in the build. The AWS services BOM imports the AWS BOM
    implementation(enforcedPlatform(libs.platform.quarkus.bom))
    //implementation("software.amazon.awssdk:sts:2.46.17")
    //implementation("software.amazon.awssdk:sso:2.46.17")
    //implementation("software.amazon.awssdk:ssooidc:2.46.17")
}