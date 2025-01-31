package no.nav.tms.soknadskvittering.setup

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.tms.kafka.application.JsonMessage

fun withMDC(jsonMessage: JsonMessage, block: () -> Unit) = withLoggingContext(
    map = mutableMapOf(
        "minside_id" to jsonMessage["soknadsId"].asText(),
        "event_name" to jsonMessage.eventName,
        "contenttype" to "soknadskvittering"
    ).also {
        jsonMessage.getOrNull("vedleggsId")?.let { vedleggsId ->
           it["vedlegg_id"] = vedleggsId.asText()
        }
    },
    body = block
)
