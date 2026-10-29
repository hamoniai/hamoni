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

package ai.hamoni.control_room

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

/**
 * Behaviour of the authentication entry points.
 */
@QuarkusTest
class AuthResourceTest {

    @Test
    @TestSecurity(user = "owner")
    fun `the login endpoint sends the browser to the default next stage`() {
        given()
            .redirects().follow(false)
            .`when`().get("/api/auth/login?provider=microsoft")
            .then()
            .statusCode(303)
            .header("Location", equalTo("/auth/organisations"))
    }

    @Test
    @TestSecurity(user = "owner")
    fun `the login endpoint sends the browser to the requested next stage`() {
        given()
            .redirects().follow(false)
            .`when`().get("/api/auth/login?provider=google&next=/inbox")
            .then()
            .statusCode(303)
            .header("Location", equalTo("/inbox"))
    }

    @Test
    @TestSecurity(user = "owner")
    fun `the login endpoint refuses to redirect outside the application`() {
        given()
            .redirects().follow(false)
            .`when`().get("/api/auth/login?next=https://example.com")
            .then()
            .statusCode(303)
            .header("Location", equalTo("/auth/organisations"))

        given()
            .redirects().follow(false)
            .`when`().get("/api/auth/login?next=//example.com")
            .then()
            .statusCode(303)
            .header("Location", equalTo("/auth/organisations"))
    }

    @Test
    fun `a protected REST call without a session returns 401`() {
        given()
            .redirects().follow(false)
            .`when`().get("/api/auth/profile")
            .then()
            .statusCode(401)
    }
}
