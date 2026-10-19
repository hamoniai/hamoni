package ai.hamoni.control_room.processor

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing


/*
 * Processors for reactive messages. Used for communication between the control room to other components.
 */

/**
 * Generator for process runner interaction events.
 */
@ApplicationScoped
class ProcessRunnerGenerator {
    @Incoming("process-runner-status-response")
    @Outgoing("process-runner-status-request")
    fun status(pingPayload: String?): String {
        if ("PING".equals(pingPayload, ignoreCase = true)) {
            return "PONG"
        }
        return "UNKNOWN"
    }

}
