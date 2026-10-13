import ai.hamoni.gradle.util.getOverridableProperty
import io.quarkus.gradle.tasks.QuarkusDev

/**
 * Conventions for Kotlin extension projects. We will assume that all Quarkus projects use Kotlin for simplicity.
 * The quarkus plugin can only be applied in test for non-core modules, otherwise there will be build errors.
 */
plugins {
    id("quarkus-conventions")
    id("io.quarkus")
    id("org.kordamp.gradle.jandex") // TODO: this needs replacing, the outputs are not Configuration Cache compliant.
}

/**
 * Dependencies for Quarkus
 */
dependencies {
    // Sync and async client for AWS services
    //implementation("software.amazon.awssdk:apache-client")
    //implementation("software.amazon.awssdk:netty-nio-client")
}

/**
 * Javadoc needs Jandex to complete first
 */
tasks.withType(Javadoc::class.java).configureEach {
    dependsOn(tasks.named("jandex"))
}

/**
 * Configure Quarkus dev to use the correct AWS SSO profiles
 */
tasks.withType<QuarkusDev>().configureEach {
    //getOverridableProperty("aws.default.profile")?.let {
    //    jvmArgs = listOf("-Daws.profile=$it")
    //}
}

/**
 * Disable the Quarkus build tasks for non-main projects. The main project has root project name without '-root'
 */
val mainProjectName = project.rootProject.name.replace("-root", "")
if (project.name != mainProjectName) {
    val quarkusDisabledTasks = listOf("quarkusBuild", "quarkusBuildAppModel", "quarkusAppPartsBuild", "quarkusDependenciesBuild")
    tasks.named { it in quarkusDisabledTasks }.configureEach {
        enabled = false
    }
}