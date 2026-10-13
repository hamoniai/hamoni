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

dependencies {
    // For frontend development
    implementation(libs.platform.quarkus.ext.quinoa)

    // Projects
    //implementation(project(":hamoni-auth-aws-common"))

    // Main runtime
    implementation("io.quarkus:quarkus-jdbc-mysql")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-reactive-mysql-client")

    // Dependencies for build
    compileOnly("io.quarkus:quarkus-hibernate-reactive-panache-kotlin") // Special case, we need access to some classes in DevMode.kt
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
