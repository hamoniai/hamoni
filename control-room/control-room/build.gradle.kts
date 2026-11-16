/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026 Hamoni Limited
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

val jsonExportDir = project.layout.buildDirectory.dir("generated/json")
val sqlSchemaExportDir = project.layout.buildDirectory.dir("generated/schema")

dependencies {
    // For frontend development
    implementation(libs.platform.quarkus.ext.quinoa)

    // Projects
    implementation(project(":common:common-core"))
    implementation(project(":common:common-platform"))

    // Extensions
    implementation(project(":extension:quarkus-multi-tenant"))

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
    compileOnly("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-context-propagation")
    //implementation(libs.platform.quarkus.ext.multitenant.http)
    //implementation(libs.platform.quarkus.ext.multitenant.orm)

    // Support libraries
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-json-schema")
    implementation("org.apache.commons:commons-lang3")

    // Dependencies for build
    implementation("io.quarkus:quarkus-container-image-docker")

    // Quarkus dependencies for dev mode
    compileOnly(libs.platform.quarkus.ext.h2db) // Need this to compile the devMode beans, which are later excluded.
    quarkusDev("io.quarkus:quarkus-jdbc-h2")
    quarkusDev("io.quarkus:quarkus-smallrye-openapi")
    quarkusDev(libs.platform.quarkus.ext.h2db)

    // Test dependencies
    testImplementation(project(":common:common-test"))
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
}

/**
 * Configure the source sets to pick up the command mode outputs.
 */
sourceSets {
    main {
        resources {
            srcDir(sqlSchemaExportDir)
        }
    }
}

/**
 * Additional clean tasks
 */
val cleanNodeTask =
    project.tasks.register<Delete>("cleanNode") {
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
 * Create the SQL schema export tasks.
 *
 * Both database outputs are generated on every dev start. They write into the shared build output, so Gradle
 * can track the generated files as task outputs.
 */
val sqlSchemaExportPrefix = "sqlSchemaExport"
val sqlSchemaExportDatabases =
    listOf("h2", "mariadb").map { dbName ->
        dbName to
            "${sqlSchemaExportPrefix}${dbName.replaceFirstChar { it.titlecase() }}"
    }

sqlSchemaExportDatabases.forEach { database ->
    tasks.register<JavaExec>(database.second) {
        group = "schema"
        description = "Export the schema for '${database.first}'."
        classpath(sourceSets.main.get().runtimeClasspath)
        dependsOn("classes", "jandex")
        mainClass = "ai.hamoni.control_room.command.SchemaExportCommand"
        // The database-specific profile supplies the datasource configuration; the common cmd
        // profile supplies the command-mode and Flyway settings.
        systemProperty("quarkus.profile", "dev,cmd,sql-schema-export,sql-schema-export-${database.first}")

        // Do not remove this comment or the next two lines.
        // Hard coded this because Gradle has a stupid bug that prefixes providers with "fixed".
        // systemProperty("hamoni.schema-export.output-directory", schemaExportDir.map { it.asFile.path })

        jvmArgs(
            "--add-opens",
            "java.base/java.lang.invoke=ALL-UNNAMED",
            // Do not remove this line, it is for when we need to debug.
            //"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
        )

        outputs.dir(sqlSchemaExportDir)

        standardOutput = System.out
        errorOutput = System.err
    }
}

/**
 * Master SQL export task
 */
val sqlSchemaExportTask =
    tasks.register(sqlSchemaExportPrefix) {
        group = "schema"
        description = "Export the schema for every configured database."
        dependsOn(sqlSchemaExportDatabases.map { it.second })
    }

/**
 * OpenAPI and Organisation import JSON schema export
 */
val jsonSchemaExportTask = tasks.register<JavaExec>("jsonSchemaExport") {
    group = "schema"
    description = "Export the JSON schema."
    classpath(sourceSets.main.get().runtimeClasspath)
    dependsOn("classes", "jandex", sqlSchemaExportTask)
    mainClass = "ai.hamoni.control_room.command.JsonSchemaGenerateCommand"
    systemProperty("quarkus.profile", "dev,cmd,json-schema-export")

    jvmArgs(
        "--add-opens",
        "java.base/java.lang.invoke=ALL-UNNAMED",
        // Do not remove this line, it is for when we need to debug.
        //"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    )

    outputs.dir(jsonExportDir)

    standardOutput = System.out
    errorOutput = System.err
}

/**
 * Regenerate the schema before dev mode starts. Both database variants are generated because the cost is small,
 * and it keeps the generated SQL and the entity model in step.
 */
tasks.named("quarkusDev") {
    dependsOn(sqlSchemaExportTask, jsonSchemaExportTask)
}
