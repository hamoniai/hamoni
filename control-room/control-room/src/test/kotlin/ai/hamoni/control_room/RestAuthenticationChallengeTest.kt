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

import io.mockk.every
import io.mockk.mockk
import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Behaviour of the REST authentication challenge.
 */
@QuarkusTest
class RestAuthenticationChallengeTest {

    private val challenge = RestAuthenticationChallenge()

    @Test
    fun `an unauthenticated REST call is challenged with 401`() {
        val context = contextFor("/api/tasks/123")

        assertThat(challenge.isJavaScriptRequest(context)).isTrue()
        assertThat(challenge.getChallenge(context).status).isEqualTo(401)
    }

    @Test
    fun `the login entry point is allowed to start the authorization flow`() {
        assertThat(challenge.isJavaScriptRequest(contextFor("/api/auth/login"))).isFalse()
        assertThat(challenge.isJavaScriptRequest(contextFor("/api/auth/login/"))).isFalse()
    }

    @Test
    fun `a non-API path is not treated as a REST challenge`() {
        assertThat(challenge.isJavaScriptRequest(contextFor("/auth/organisations"))).isFalse()
    }

    private fun contextFor(path: String): RoutingContext {
        val request = mockk<HttpServerRequest>()
        every { request.path() } returns path
        val context = mockk<RoutingContext>()
        every { context.request() } returns request
        return context
    }
}
