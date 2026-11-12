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

plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.platform.java.get())
    }
}

/**
 * Configure the JAR build to enable the Java Agent
 */
tasks.withType<Jar> {
    archiveBaseName.set("quarkus-multi-tenant-agent")
    archiveVersion.set("")
    manifest {
        attributes(mapOf(
            "Premain-Class" to "ai.hamoni.buildlogic.multitenant.QuarkusMultiTenantAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
        ))
    }
}

/**
 * Copy the resulting build to an
 */
val libsDir = rootProject.layout.projectDirectory.dir("libs")
tasks.register<Copy>("copyJar") {
    from(tasks.named("jar"))
    into(libsDir)
}
tasks.named("build") {
    dependsOn("copyJar")
}

/**
 * Clean the output files
 */
/**
 * Register a top level clean as only a wrapper
 */
tasks.register<Delete>("cleanJar") {
    delete(libsDir)
}
tasks.named("clean") {
    dependsOn("cleanJar")
}