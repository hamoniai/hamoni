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
package ai.hamoni.control_room.command

import ai.hamoni.common.core.REQUEST_ORGANISATION_ID
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import io.quarkus.smallrye.openapi.OpenApiFilter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.OpenAPI
import org.eclipse.microprofile.openapi.models.Operation
import org.eclipse.microprofile.openapi.models.media.Schema
import org.eclipse.microprofile.openapi.models.parameters.Parameter
import org.eclipse.microprofile.openapi.models.security.SecurityScheme
import org.jboss.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Generate the organisation import JSON Schema from the generated OpenAPI
 * document.
 *
 * This command does not generate the OpenAPI document; that is produced by the
 * OpenAPI export in advance. It reads the organisation document, extracts the
 * `Organisation` component and its dependencies, and writes a standalone JSON
 * Schema for the import file.
 */
@QuarkusMain(name = "json-schema-export")
@ApplicationScoped
@IfBuildProfile("json-schema-export")
class JsonSchemaGenerateCommand
@Inject
constructor(private val jsonSchemaExportService: JsonSchemaExportService) :
    QuarkusApplication {
    private val logger = Logger.getLogger(JsonSchemaGenerateCommand::class.java)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LaunchMode.set(LaunchMode.NORMAL)
            Quarkus.run(JsonSchemaGenerateCommand::class.java, *args)
            Quarkus.asyncExit()
        }
    }

    /**
     * Run the export and return zero when the schema was written.
     */
    override fun run(vararg args: String): Int {
        val output = jsonSchemaExportService.generate()
        logger.infof("Generated JSON schema: %s", output)
        return 0
    }
}

/**
 * Build the import JSON Schema from the organisation OpenAPI document.
 */
@ApplicationScoped
@IfBuildProfile("json-schema-export")
class JsonSchemaExportService
@Inject
constructor(
    @param:ConfigProperty(name = "hamoni.json-schema-export.input")
    private val inputPath: String,
    @param:ConfigProperty(name = "hamoni.json-schema-export.output")
    private val outputPath: String,
    @param:ConfigProperty(name = "hamoni.json-schema-export.version")
    private val schemaVersion: String,
) {
    companion object {
        private const val REF_PREFIX = "#/components/schemas/"
        private const val ROOT_SCHEMA = "Organisation"
        private const val ORGANISATION_WITH_IDENTIFIER = "OrganisationWithIdentifier"
    }

    private val json = Json { prettyPrint = true }

    /**
     * Generate the schema and return the path it was written to.
     */
    fun generate(): Path {
        val document = json.parseToJsonElement(Files.readString(Path.of(inputPath))).jsonObject
        val schemas =
            document["components"]
                ?.jsonObject
                ?.get("schemas")
                ?.jsonObject
                ?: throw IllegalStateException("OpenAPI document '$inputPath' has no components.schemas")

        val definitions = linkedMapOf<String, JsonObject>()
        collectSchema(ROOT_SCHEMA, schemas, definitions)
        definitions[ORGANISATION_WITH_IDENTIFIER] = organisationWithIdentifier(definitions.getValue(ROOT_SCHEMA))

        val schema =
            buildJsonObject {
                put("\$schema", JsonPrimitive("https://json-schema.org/draft/2020-12/schema"))
                put("\$id", JsonPrimitive("https://hamoni.ai/schema/organisation-init/$schemaVersion"))
                put("type", JsonPrimitive("object"))
                put("additionalProperties", JsonPrimitive(false))
                put(
                    "required",
                    buildJsonArray {
                        add(JsonPrimitive("schemaVersion"))
                        add(JsonPrimitive("defaultOrganisation"))
                    }
                )
                put(
                    "properties",
                    buildJsonObject {
                        put("schemaVersion", buildJsonObject { put("const", JsonPrimitive(schemaVersion)) })
                        put(
                            "defaultOrganisation",
                            buildJsonObject {
                                put("\$ref", JsonPrimitive("#/\$defs/$ROOT_SCHEMA"))
                            }
                        )
                        put(
                            "organisations",
                            buildJsonObject {
                                put("type", JsonPrimitive("array"))
                                put(
                                    "items",
                                    buildJsonObject {
                                        put("\$ref", JsonPrimitive("#/\$defs/$ORGANISATION_WITH_IDENTIFIER"))
                                    }
                                )
                            }
                        )
                    }
                )
                put("\$defs", JsonObject(definitions.mapValues { (_, value) -> rewriteReferences(value) }))
            }

        val output = Path.of(outputPath)
        output.parent?.createDirectories()
        Files.writeString(output, json.encodeToString(JsonElement.serializer(), schema))
        return output
    }

    /**
     * Add the identifier required by a non-default organisation.
     */
    private fun organisationWithIdentifier(organisation: JsonObject): JsonObject {
        val properties = organisation["properties"]?.jsonObject
            ?: throw IllegalStateException("Organisation schema has no properties")
        val required =
            organisation["required"]?.jsonArray
                ?.map { it.jsonPrimitive.content }
                .orEmpty()

        return JsonObject(
            organisation.toMutableMap().apply {
                put(
                    "properties",
                    JsonObject(
                        properties + (
                            "id" to buildJsonObject {
                                put("type", JsonPrimitive("string"))
                                put("format", JsonPrimitive("uuid"))
                            }
                            )
                    )
                )
                put(
                    "required",
                    JsonArray((required + "id").distinct().map { JsonPrimitive(it) })
                )
            }
        )
    }

    /**
     * Collect [name] and every schema it references, removing read-only
     * properties and nullable fields from `required`.
     */
    private fun collectSchema(name: String, schemas: JsonObject, definitions: MutableMap<String, JsonObject>) {
        if (definitions.containsKey(name)) return

        val schema =
            schemas[name]?.jsonObject
                ?: throw IllegalStateException("OpenAPI schema '$name' not found")

        val cleaned = stripNullableRequired(stripReadOnly(schema))
        definitions[name] = cleaned

        references(cleaned).forEach { reference -> collectSchema(reference, schemas, definitions) }
    }

    /**
     * Remove properties marked `readOnly` and drop them from `required`.
     */
    private fun stripReadOnly(schema: JsonObject): JsonObject {
        val properties = schema["properties"]?.jsonObject ?: return schema
        val readOnly =
            properties
                .filterValues { property ->
                    property is JsonObject && property["readOnly"]?.jsonPrimitive?.booleanOrNull == true
                }
                .keys

        if (readOnly.isEmpty()) return schema

        val required =
            schema["required"]?.jsonArray
                ?.filterNot { it.jsonPrimitive.content in readOnly }
                ?.let { JsonArray(it) }

        return JsonObject(
            schema.toMutableMap().apply {
                this["properties"] = JsonObject(properties.filterKeys { it !in readOnly })
                if (!required.isNullOrEmpty()) {
                    this["required"] = required
                } else {
                    remove("required")
                }
            }
        )
    }

    /**
     * Remove nullable properties from `required`.
     */
    private fun stripNullableRequired(schema: JsonObject): JsonObject {
        val properties = schema["properties"]?.jsonObject ?: return schema
        val nullable =
            properties
                .filterValues { property -> property.isNullable() }
                .keys

        if (nullable.isEmpty()) return schema

        val required =
            schema["required"]?.jsonArray
                ?.filterNot { it.jsonPrimitive.content in nullable }
                ?.let { JsonArray(it) }

        return JsonObject(
            schema.toMutableMap().apply {
                if (!required.isNullOrEmpty()) {
                    this["required"] = required
                } else {
                    remove("required")
                }
            }
        )
    }

    /**
     * Whether this schema allows `null`.
     */
    private fun JsonElement.isNullable(): Boolean {
        val schema = this as? JsonObject ?: return false

        val anyOf = schema["anyOf"]?.jsonArray
        if (anyOf?.any { it is JsonObject && it["type"]?.jsonPrimitive?.contentOrNull == "null" } == true) {
            return true
        }

        val type = schema["type"]
        return type is JsonArray && type.any { it.jsonPrimitive.contentOrNull == "null" }
    }

    /**
     * Collect the names of every `components/schemas` reference in [element].
     */
    private fun references(element: JsonElement): Set<String> = when (element) {
        is JsonObject -> {
            element.entries.flatMap { (key, value) ->
                if (key == "\$ref") {
                    val reference = (value as? JsonPrimitive)?.contentOrNull
                    if (reference != null && reference.startsWith(REF_PREFIX)) {
                        listOf(reference.removePrefix(REF_PREFIX))
                    } else {
                        emptyList()
                    }
                } else {
                    references(value).toList()
                }
            }.toSet()
        }

        is JsonArray -> {
            element.flatMap { references(it) }.toSet()
        }

        else -> {
            emptySet()
        }
    }

    /**
     * Rewrite `components/schemas` references to `$defs` references.
     */
    private fun rewriteReferences(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            JsonObject(
                element.mapValues { (key, value) ->
                    if (key == "\$ref") {
                        val reference = (value as? JsonPrimitive)?.contentOrNull
                        if (reference != null && reference.startsWith(REF_PREFIX)) {
                            JsonPrimitive("#/\$defs/${reference.removePrefix(REF_PREFIX)}")
                        } else {
                            value
                        }
                    } else {
                        rewriteReferences(value)
                    }
                }
            )
        }

        is JsonArray -> {
            JsonArray(element.map { rewriteReferences(it) })
        }

        else -> {
            element
        }
    }
}

/**
 * Shared security documentation for the Control Room API documents.
 */
private object OpenApiSecurity {
    private const val BEARER = "bearerAuth"
    private const val COOKIE = "cookieAuth"

    /**
     * Add the bearer and browser-session security schemes to [openAPI].
     */
    fun addSchemes(openAPI: OpenAPI) {
        val components = openAPI.components ?: OASFactory.createComponents().also { openAPI.components = it }
        val schemes = components.securitySchemes

        if (schemes == null || !schemes.containsKey(BEARER)) {
            components.addSecurityScheme(
                BEARER,
                OASFactory.createSecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("A bearer token issued by the organisation's OpenID Connect provider.")
            )
        }

        if (schemes == null || !schemes.containsKey(COOKIE)) {
            components.addSecurityScheme(
                COOKIE,
                OASFactory.createSecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("q_session")
                    .description("The browser session cookie created by the OpenID Connect login flow.")
            )
        }
    }

    /**
     * Add the bearer-or-cookie requirement to [operation], unless it already
     * documents one of them.
     */
    fun addRequirement(operation: Operation) {
        val requirements = operation.security.orEmpty().toMutableList()
        if (requirements.none { it.hasScheme(BEARER) || it.hasScheme(COOKIE) }) {
            requirements.add(OASFactory.createSecurityRequirement().addScheme(BEARER))
            requirements.add(OASFactory.createSecurityRequirement().addScheme(COOKIE))
        }
        operation.security = requirements
    }
}

/**
 * Documents the organisation header and authentication on the organisation API
 * document.
 */
@OpenApiFilter(documentNames = ["org"])
class OrganisationOpenApiFilter : OASFilter {
    override fun filterOpenAPI(openAPI: OpenAPI) {
        OpenApiSecurity.addSchemes(openAPI)

        openAPI.paths?.pathItems?.values?.forEach { pathItem ->
            pathItem.operations.values.forEach { operation ->
                operation.addOrganisationHeader()
                OpenApiSecurity.addRequirement(operation)
            }
        }
    }

    /**
     * Add the organisation header parameter to [operation] when it is absent.
     */
    private fun Operation.addOrganisationHeader() {
        val alreadyPresent =
            parameters.orEmpty().any {
                it.name == REQUEST_ORGANISATION_ID && it.`in` == Parameter.In.HEADER
            }
        if (alreadyPresent) return

        addParameter(
            OASFactory.createParameter()
                .name(REQUEST_ORGANISATION_ID)
                .`in`(Parameter.In.HEADER)
                .required(true)
                .description("The identifier of the organisation whose data is being accessed.")
                .schema(
                    OASFactory.createSchema()
                        .addType(Schema.SchemaType.STRING)
                        .format("uuid")
                )
        )
    }
}

/**
 * Documents authentication on the platform API document.
 *
 * The login entry point and the public identity-provider list do not require
 * authentication; the remaining operations do.
 */
@OpenApiFilter(documentNames = ["pfm"])
class PlatformOpenApiFilter : OASFilter {
    override fun filterOpenAPI(openAPI: OpenAPI) {
        OpenApiSecurity.addSchemes(openAPI)

        openAPI.paths?.pathItems?.forEach { (path, pathItem) ->
            if (path.isPublic()) {
                pathItem.operations.values.forEach { it.security = emptyList() }
                return@forEach
            }
            pathItem.operations.values.forEach { OpenApiSecurity.addRequirement(it) }
        }
    }

    /**
     * Whether [path] is one of the public platform entry points.
     */
    private fun String.isPublic(): Boolean = endsWith("/auth/login") || endsWith("/auth/identityProviders")
}
