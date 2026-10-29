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

package ai.hamoni.control_room.resource

import io.quarkus.oidc.RefreshToken
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.resteasy.reactive.RestResponse

/*
 * Temporary endpoints for testing the OIDC setup
 */

@Path("/tokens")
class TokenResource @Inject constructor(
        private val idToken: JsonWebToken,
        private val accessToken: JsonWebToken,
        private val refreshToken: RefreshToken
    ) {

    /**
     * Returns the tokens available to the application.
     * This endpoint exists only for demonstration purposes.
     * Do not expose these tokens in a real application.
     *
     * @return an HTML page containing the tokens available to the application.
     */
    @GET
    @Path("/inspect")
    @Produces("text/html")
    fun profileOrganisations(): Uni<RestResponse<String>> {
        val response = """
        """.trimIndent()

        return Uni.createFrom().item {
            val tokens = idToken.claimNames.joinToString("\n") { "<li>${it} (${it.javaClass.name}): <li>" }
            RestResponse.ResponseBuilder.create<String>(RestResponse.Status.OK)
                .entity("""
                    <html>
                        <body>
                            <ul>
                                <li>issuer: ${idToken.issuer}</li>
                                <li>issuer: ${idToken.subject}</li>
                                $tokens
                            </ul>
                        </body>
                    </html>
                """.trimIndent())
                .build()
        }
    }
}
