package no.nav.tms.soknadskvittering.aggregation

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadFerdigstiltSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknadFerdigstilt")
        .withFields("soknadsId", "produsent")
        .withOptionalFields("metadata")

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val soknadsId = jsonMessage["soknadsId"].asText()

        repository.markerFerdigstilt(soknadsId).let { wasUpdated ->

            if (wasUpdated) {
                log.info { "Markerte soknadskvittering som ferdigstilt" }
            } else {
                log.warn { "Fant ikke soknadskvittering som skulle markeres som ferdigstilt" }
            }
        }
    }
}
