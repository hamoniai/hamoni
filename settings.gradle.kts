includeBuild("build-logic")
rootProject.name="hamoni"

val rootProjectDir = rootProject.projectDir

// Add all projects found dynamically, but collapse all to the top level.
rootProjectDir.walkTopDown()
    .maxDepth(4)
    .onEnter { file ->
        !(file.name.startsWith(".") || file.name == "build-logic" || file.name == "build" || file.name == "gradle")
    }
    .filter { file ->
        file.name == "build.gradle.kts" && file.parentFile != rootProjectDir
    }
    .forEach { buildFile ->
        val projectDir = buildFile.parentFile
        val projectName = ":${projectDir.name}"
        include(projectName)
        project(projectName).projectDir = projectDir
    }
