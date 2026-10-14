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

    // Dependencies for build
    implementation("io.quarkus:quarkus-container-image-docker")

    // Quarkus dependencies for dev mode
    compileOnly(libs.platform.quarkus.ext.h2db) // Need this to compile the devMode beans, which are later excluded.
    quarkusDev("io.quarkus:quarkus-jdbc-h2")
    quarkusDev(libs.platform.quarkus.ext.h2db)

    // Test dependencies
    testImplementation("io.quarkus:quarkus-jdbc-h2")
    testImplementation(libs.platform.quarkus.ext.h2db)
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
}

///**
// * Additional clean tasks
// */
//val cleanNodeTask = project.tasks.register<Delete>("cleanNode") {
//    val base = project.layout.projectDirectory.dir("src/main/webui")
//    delete(
//        base.dir("node_modules"),
//        base.dir("dist"),
//        base.dir(".angular"),
//    )
//}
//project.tasks.named("clean").configure {
//    dependsOn(cleanNodeTask)
//}
