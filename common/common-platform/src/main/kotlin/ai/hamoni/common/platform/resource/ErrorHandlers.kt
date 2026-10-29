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

package ai.hamoni.common.platform.resource

import ai.hamoni.common.platform.APPLICATION_PROBLEM_JSON_UTF8
import ai.hamoni.common.platform.APPLICATION_PROBLEM_JSON_UTF8_TYPE
import ai.hamoni.common.platform.PROBLEM_TYPE_PREFIX
import ai.hamoni.common.platform.PRODUCT_NAME
import ai.hamoni.common.platform.REQUEST_PROPERTY_ID
import ai.hamoni.common.core.ResourceLogger
import ai.hamoni.common.platform.data.Problem
import ai.hamoni.common.platform.data.ValidationError
import ai.hamoni.common.core.exception.EntityNotFoundException
import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.common.core.exception.SecurityException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Produces
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod


/**
 * Request ID filter to add tracking
 */
class Filters {
    private val encoder = Base64.getEncoder()

    /**
     * Generate a random 8 character request identifier. Not cryptographically safe, only for tracking.
     */
    @ServerRequestFilter(preMatching = true, nonBlocking = true)
    fun requestIdRequestFilter(requestContext: ContainerRequestContext) {
        val requestIdBytes = ByteArray(6)
        ThreadLocalRandom.current().nextBytes(requestIdBytes)
        requestContext.setProperty(REQUEST_PROPERTY_ID, encoder.encodeToString(requestIdBytes))
    }

    /**
     * Add the request identifier to the response for tracking.
     */
    @ServerResponseFilter
    fun requestIdResponseFilter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        responseContext.headers.add(REQUEST_PROPERTY_ID,
            requestContext.getProperty(REQUEST_PROPERTY_ID))
    }
}


/**
 * Exception mappers
 */
class ExceptionMappers {
    fun getRequestId(context: ContainerRequestContext) =
        (context.getProperty(REQUEST_PROPERTY_ID) as? String) ?: "<UNKNOWN>"

    fun getServiceName(source: KFunction<Any?>): String {
        return "${source.javaMethod!!.declaringClass.simpleName}#${source.name}"
    }

    fun createErrorInstance(context: ContainerRequestContext, code: Int) =
        if (context.hasProperty(REQUEST_PROPERTY_ID)) {
            "urn:${PRODUCT_NAME}:error?requestId=${context.getProperty(REQUEST_PROPERTY_ID)}&code=${code}"
        } else {
            "urn:${PRODUCT_NAME}:error?code=${code}"
        }

    // Service exceptions
    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleConstraintViolationException(exception: ConstraintViolationException, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        val message = exception.constraintViolations
            .joinToString(
                separator = ". ",
                postfix = ".",
                prefix = "The following fields were in error: "
            ) { v -> v.message }
        ResourceLogger.LOGGER.restValidationError(reference, "<unknown>", message)

        val errorStatus = RestResponse.Status.BAD_REQUEST
        val errorObject: Problem = Problem.ValidationProblem(
            "$PROBLEM_TYPE_PREFIX/bad-request",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            exception.message ?: errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode),
            exception.constraintViolations.map { e ->
                ValidationError(e.message, e.propertyPath.toString())
            }
        )

        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }

    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleNotFoundExceptionMapper(exception: EntityNotFoundException, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        ResourceLogger.LOGGER.restNotFoundError(reference, getServiceName(exception.serviceMethod))

        val errorStatus = RestResponse.Status.NOT_FOUND
        val errorObject: Problem = Problem.GenericProblem(
            "$PROBLEM_TYPE_PREFIX/not-found",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            exception.message ?: errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode)
        )
        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }

    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleInvalidParameterExceptionMapper(exception: InvalidParameterException, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        ResourceLogger.LOGGER.formatError(reference, exception.field, exception.message)

        val errorStatus = RestResponse.Status.BAD_REQUEST
        val errorObject: Problem = Problem.GenericProblem(
            "$PROBLEM_TYPE_PREFIX/bad-request",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            exception.message ?: errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode)
        )
        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }

    /**
     * Handle an authorisation exception. Determine if we want to show the detail.
     */
    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleSecurityExceptionMapper(exception: SecurityException, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        ResourceLogger.LOGGER.restSecurityError(reference, getServiceName(exception.serviceMethod), exception.message)

        val errorStatus = RestResponse.Status.FORBIDDEN
        val errorObject: Problem = Problem.GenericProblem(
            "$PROBLEM_TYPE_PREFIX/forbidden",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            exception.message ?: errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode)
        )
        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }

    // Framework exceptions
    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleResourceNotFoundExceptionMapper(exception: NotFoundException, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        ResourceLogger.LOGGER.restNotFoundError(reference, context.uriInfo.requestUri.toString())

        val errorStatus = RestResponse.Status.NOT_FOUND
        val errorObject: Problem = Problem.GenericProblem(
            "$PROBLEM_TYPE_PREFIX/not-found",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            exception.message ?: errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode)
        )
        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }

    /**
     * This is a bit special. The Hibernate validator does this when it fails to validate a bean. The issue is that
     * when accessing the isValid() method of the validators which is annotated with @AssertTrue (to create a custom)
     * message, then the error gets wrapped in a InvocationTargetException before Hibernate sees it. This is then
     * cause and remapped to a AttributeExtractionException. So, here we look at the cause, if it is really a constraint
     * problem, then map the error. If not then just handle as an unknown problem.
     *
     * The better way to fix this would be to redesign the argument validation, but the way it works at the moment is
     * simple and elegant. Using it in the annotation driven way would make it a lot more complicated.
     *
     * This approach does also mean double reflective access to isValid, which is not very nice.
     *
     * The actual ConstraintValidation is wrapped further up in the exception hierarchy.
     */
    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleValidationExceptionMapper(exception: ValidationException, context: ContainerRequestContext): RestResponse<Problem> {
        var e = exception.cause
        while (e != null) {
           if (e is ConstraintViolationException) {
               return handleConstraintViolationException(e, context)
           }
            e = e.cause
        }
        return handleExceptionMapper(exception, context)
    }

    // General handler - do not display the message here, it is not sanitised
    @ServerExceptionMapper
    @Produces(APPLICATION_PROBLEM_JSON_UTF8)
    fun handleExceptionMapper(exception: Exception, context: ContainerRequestContext): RestResponse<Problem> {
        val reference = getRequestId(context)
        ResourceLogger.LOGGER.restError(reference, exception)

        val errorStatus = RestResponse.Status.INTERNAL_SERVER_ERROR
        val errorObject: Problem = Problem.GenericProblem(
            "$PROBLEM_TYPE_PREFIX/internal-server-error",
            errorStatus.reasonPhrase,
            errorStatus.statusCode,
            errorStatus.reasonPhrase,
            createErrorInstance(context, errorStatus.statusCode)
        )
        return RestResponse.ResponseBuilder.create(errorStatus, errorObject)
            .type(APPLICATION_PROBLEM_JSON_UTF8_TYPE)
            .build()
    }
}