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

import ai.hamoni.gradle.tasks.GeneratePomPropertiesFileTask
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.register

val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Conventions for Java projects
 */
plugins {
    id("base-conventions")
    java
    idea
    `maven-publish`
}

dependencies {
    val assertjDep = libs.findLibrary("test.assertj.core").get()
    testImplementation(assertjDep)
}

/**
 * IntelliJ setup
 */
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

/**
 * Java projects setup
 */
val javaLanguageVersion = 25
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaLanguageVersion)
    }
    withJavadocJar()
    withSourcesJar()
}

/**
 * Standard configuration for java tasks.
 */
tasks.withType<JavaCompile>().configureEach {
    options.isDeprecation = true
    options.isWarnings = true
    options.encoding = "UTF-8"
    options.compilerArgs.add("-implicit:none")
    // These are useful for getting more information into the logs
    //options.isDebug = true
    //options.debugOptions.debugLevel = "source,lines,vars"
    //options.isVerbose = true
}

/**
 * Standard configuration for test tasks
 */
tasks.withType<Test>().configureEach {
    // Using JUnitPlatform for running tests
    useJUnitPlatform()
    minHeapSize = "256m" // initial heap size
    maxHeapSize = "4096m" // maximum heap size

    // System truststore integration
    if (OperatingSystem.current().isWindows) {
        systemProperty("javax.net.ssl.trustStore", "c:\\Windows\\win.ini")
        systemProperty("javax.net.ssl.trustStoreType", "Windows-ROOT")
    } else if (OperatingSystem.current().isMacOsX) {
        systemProperty("javax.net.ssl.trustStore", "/dev/null")
        systemProperty("javax.net.ssl.trustStoreType", "KeychainStore")
    }

    // Proxy integration
    systemProperty("java.net.useSystemProxies", "true")

    // Testing
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/**
 * Standard configuration for Javadoc tasks
 */
tasks.withType<Javadoc>().configureEach {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

/**
 * Standard configuration for publishing, especially to produce the pom.xml/pom.properties
 */
publishing {
    publications {
        create<MavenPublication>("default") {
            from(project.components["java"])
            pom {
                packaging = "jar"
                licenses {
                    license {
                        name = "Hamoni Commercial License v1.0"
                    }
                }
            }
        }
    }
}

/**
 * Create an additional task to produce a pom.properties file
 */
tasks.register<GeneratePomPropertiesFileTask>("generatePomPropertiesFile")

/**
 * Generate the pom.xml and pom.properties before the Java compilation so that the annotation processors have
 * access to the POM.
 */
tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.named { it.startsWith("generatePomFileFor") || it == "generatePomPropertiesFile"})
}

/**
 * Exclude dev resources from the resources task
 */
tasks.withType<ProcessResources>().configureEach {
    exclude("**/.gitkeep")
}

/**
 * Configure any JAR task to include the correct contents
 */
tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version.toString()
        attributes["Created-By"] = "Gradle JAR plugin ${GradleVersion.current().version}"
        attributes["Java-Version"] = javaLanguageVersion.toString()
        attributes["Build-Jdk-Spec"] = JavaVersion.current().majorVersion
    }
    exclude("**/.gitkeep")
}