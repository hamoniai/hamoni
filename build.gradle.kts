group = "ai.hamoni"
version = "1.0.0-SNAPSHOT"

/**
 * Register a top level clean as only a wrapper
 */
tasks.register<Delete>("clean") {
    delete(project.layout.buildDirectory)
}
