/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026  Hamoni Limited
 *
 * Unless you have received this program directly from Hamoni Limited pursuant to
 * the terms of a commercial license agreement with Hamoni Limited, then this
 * program is licensed to you under the terms of version 3 of the GNU Affero
 * General Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.internal.KaptTask
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration

val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Determine if this is a runtime or deployment project
val isDeployment = project.name.endsWith("-deployment")

/**
 * Conventions for Kotlin extension projects. We will assume that all Quarkus projects use Kotlin for simplicity.
 */
plugins {
    id("quarkus-conventions")
    id("org.kordamp.gradle.jandex")
}

// If the runtime project, add the extension plugin
if (!isDeployment) {
    plugins.apply("io.quarkus.extension")
    project.extensions.configure<QuarkusExtensionConfiguration> {
        deploymentModule = "${project.name}-deployment"
    }
}

/**
 * Dependencies for Quarkus
 */
dependencies {
    // Need to use kapt to generate stubs which can then be compiled by the annotation processors.
    kapt("io.quarkus:quarkus-extension-processor")

    testImplementation("io.quarkus:quarkus-junit-internal")
}

/**
 * Javadoc needs Jandex to complete first
 */
tasks.withType(Javadoc::class.java).configureEach {
    dependsOn(tasks.named("jandex"))
}

/**
 * Hack to put the pom.xml in the correct place for the extension annotation processor, which expects a Maven layout
 */
val pomCopierTask = tasks.register<Copy>("pomCopier") {
    into(project.layout.buildDirectory.map { it.asFile.toPath().resolve("tmp/kapt3").toFile() })
    from(tasks.named("generatePomFileForDefaultPublication")) {
        rename(".*", "pom.xml")
    }
}
tasks.withType<KaptTask>().configureEach {
    dependsOn(pomCopierTask, tasks.named { it.startsWith("generatePomFileFor") || it == "generatePomPropertiesFile"})
}

/**
 * The Quarkus extension plugin has an interesting "bug". It creates a model of all the dependencies so that it can
 * Quarkus. However, it only uses the main source set from the deployment module project. It looks at the sources and
 * resources, however, the kapt plugin generates the BuildStep index outside of this. As such, we need to do some really
 * hacky looping.
 *
 * We need to loop the output of kapt back into the main resources. This breaks things a little, however, it is
 * lucky that we use using kapt which seperates javaCompile and the annotation processor. The information needs
 * to be ready for test, however, once JavaCompile runs, the sourceset will be locked.
 *
 * @see io.quarkus.gradle.tooling.GradleApplicationModelBuilder#getProjectArtifact(Project, boolean)
 */
if (isDeployment) {
    val kaptGeneratedSources = project.layout.buildDirectory.dir("generated/sources/kapt/resources/main")
    // Stage the files we want
    val kaptResourcesCopyTask = tasks.register<Copy>("kaptResourcesCopy") {
        into(kaptGeneratedSources)
        from(project.tasks.named("kaptKotlin")) {
            include("META-INF/**")
        }
    }
    // Add to the main SourceSet resources
    sourceSets.main.configure {
        resources.srcDir(kaptResourcesCopyTask)
    }
    // Prevent the duplicate from appearing in the JAR (as kapt and processResources both feed the jar).
    // This is a lazy approach, a better one would be to exclude specific items. However, we know that the
    // items we added to the SourceSet should be last.
    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}