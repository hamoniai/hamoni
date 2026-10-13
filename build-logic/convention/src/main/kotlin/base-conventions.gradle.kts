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

/**
 * Base conventions applicable to all types of project
 */

// Pre-check that the runtime is correct
if (JavaVersion.current().majorVersion.toInt() < JavaVersion.VERSION_25.majorVersion.toInt()) {
    throw GradleException("Java version error. Project ${project.name} must run with Java 25 or higher. Currently using ${JavaVersion.current()}")
}

project.group = project.rootProject.group
project.version = project.rootProject.version

/**
 * Repository configuration for all projects
 */
repositories {
    mavenCentral()
    mavenLocal()
}
