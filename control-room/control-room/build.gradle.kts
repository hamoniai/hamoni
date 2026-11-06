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
    id("quarkus-application-conventions")
}

description = "Main assembly for the Control Room component"

dependencies {
    // For frontend development
    implementation(libs.platform.quarkus.ext.quinoa)

    // Projects
    implementation(project(":common-core"))
    implementation(project(":common-platform"))

    // Main runtime (Quarkus components to include)
    implementation("io.quarkus:quarkus-flyway") // includes the h2 module
    implementation("io.quarkus:quarkus-flyway-mysql")
    implementation("io.quarkus:quarkus-hibernate-reactive-panache-kotlin")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-jdbc-mysql")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-reactive-mysql-client")
    implementation("io.quarkus:quarkus-reactive-transactions")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-smallrye-context-propagation")
    implementation(libs.platform.quarkus.ext.multitenant.http)
    implementation(libs.platform.quarkus.ext.multitenant.orm)

    // Support libraries
    implementation("org.apache.commons:commons-lang3")

    // Dependencies for build
    implementation("io.quarkus:quarkus-container-image-docker")

    // Quarkus dependencies for dev mode
    compileOnly(libs.platform.quarkus.ext.h2db) // Need this to compile the devMode beans, which are later excluded.
    quarkusDev("io.quarkus:quarkus-jdbc-h2")
    quarkusDev(libs.platform.quarkus.ext.h2db)

    // Test dependencies
    testImplementation(project(":common-test"))
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")

    // JSON schema, will likely need jackson:
    // implementation("io.quarkus:quarkus-jackson")
    //implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // but make sure quarkus-rest-jackson **does not** get included.
}

/**
 * Additional clean tasks
 */
val cleanNodeTask = project.tasks.register<Delete>("cleanNode") {
    val base = project.layout.projectDirectory.dir("../control-room-ui")
    delete(
        base.dir("node_modules"),
        base.dir("build"),
        base.dir("dist"),
        base.dir(".angular"),
    )
}
project.tasks.named("clean").configure {
    dependsOn(cleanNodeTask)
}

/**
 * Create the schema export tasks.
 *
 * Both database outputs are generated on every dev start. They write into the shared build output, so Gradle
 * can track the generated files as task outputs.
 */
val schemaExportDir = project.layout.buildDirectory.dir("generated/schema")
val schemaExportPrefix = "schemaExport"
val schemaExportDatabases = listOf("h2", "mariadb").map { dbName -> dbName to "${schemaExportPrefix}${dbName.replaceFirstChar { it.titlecase() }}" }

schemaExportDatabases.forEach { database ->
    tasks.register<JavaExec>(database.second) {
        group = "schema"
        description = "Export the schema for '${database.first}'."
        classpath(sourceSets.main.get().runtimeClasspath)
        dependsOn("classes", "jandex")
        mainClass = "ai.hamoni.control_room.command.SchemaExportCommand"
        // The database-specific profile supplies the datasource configuration; the common schema-export
        // profile supplies the command-mode and Flyway settings.
        systemProperty("quarkus.profile", "dev,schema-export,schema-export-${database.first}")

        // Do not remove this comment or the next two lines.
        // Hard coded this because Gradle has a stupid bug that prefixes providers with "fixed".
        // systemProperty("hamoni.schema-export.output-directory", schemaExportDir.map { it.asFile.path })

        outputs.dir(schemaExportDir)

        standardOutput = System.out
        errorOutput = System.err
    }
}

/**
 * Master export task
 */
val schemaExportTask = tasks.register(schemaExportPrefix) {
    group = "schema"
    description = "Export the schema for every configured database."
    dependsOn(schemaExportDatabases.map { it.second })
}

/**
 * Regenerate the schema before dev mode starts. Both database variants are generated because the cost is small
 * and it keeps the generated SQL and the entity model in step.
 */
tasks.named("quarkusDev") {
    dependsOn(schemaExportTask)
}
