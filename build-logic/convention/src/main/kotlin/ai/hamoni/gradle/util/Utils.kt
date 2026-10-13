package ai.hamoni.gradle.util

import org.gradle.api.Project

fun Project.getOverridableProperty(key: String): String? {
    val envKey = key.uppercase().replace(".", "_")
    return System.getenv(envKey) ?: providers.systemProperty(key).orNull ?: project.findProperty(key) as String?
}
