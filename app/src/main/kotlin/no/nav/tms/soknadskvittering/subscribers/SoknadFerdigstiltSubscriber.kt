package no.nav.tms.soknadskvittering.subscribers

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadFerdigstiltSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknad_ferdigstilt")
        .withFields("soknadsId")

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
