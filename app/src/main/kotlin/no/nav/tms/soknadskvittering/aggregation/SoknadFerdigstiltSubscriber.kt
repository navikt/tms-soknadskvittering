package no.nav.tms.soknadskvittering.aggregation

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadFerdigstiltSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknadFerdigstilt")
        .withFields("soknadsId", "produsent")
        .withOptionalFields("metadata")

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {

        val ferdigstiltEvent: SoknadEvent.SoknadFerdigstilt = objectMapper.treeToValue(jsonMessage.json)

        val soknad = repository.getSoknadsKvittering(ferdigstiltEvent.soknadsId)

        if (soknad == null) {
            log.warn { "Fant ikke søknad som skulle markeres som ferdigstilt" }
        } else if (soknad.ferdigstilt != null) {
            log.info { "Søknad er allerede markert fedigstilt" }
        } else {
            repository.markerFerdigstilt(ferdigstiltEvent.soknadsId)

            historikkAppender.soknadFerdigstilt(ferdigstiltEvent)
            log.info { "Markerte søknad som ferdigstilt" }
        }
    }
}
