/**
 * Base conventions applicable to all types of project
 */

// Pre-check that the runtime is correct
if (JavaVersion.current().majorVersion.toInt() < JavaVersion.VERSION_25.majorVersion.toInt()) {
    throw GradleException("Java version error. Project ${project.name} must run with Java 25 or higher. Currently using ${JavaVersion.current()}")
}

project.group = project.rootProject.group
project.version = project.rootProject.version

/**
 * Repository configuration for all projects
 */
repositories {
    mavenCentral()
    mavenLocal()
}
