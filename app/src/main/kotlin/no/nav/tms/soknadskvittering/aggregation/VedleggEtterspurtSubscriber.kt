package no.nav.tms.soknadskvittering.aggregation

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.SoknadsKvittering
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.withMDC

class VedleggEtterspurtSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedleggEtterspurt")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "brukerErAvsender",
            "tittel",
            "tidspunktEtterspurt",
            "produsent"
        )
        .withOptionalFields(
            "linkEttersending",
            "beskrivelse",
            "metadata"
        )

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {

        val event: SoknadEvent.VedleggEtterspurt = objectMapper.treeToValue(jsonMessage.json)

        repository.getSoknadsKvittering(event.soknadsId)?.let {
            leggTilEtterspurtVedlegg(it, event)
        } ?: run {
            log.warn { "Fant ikke soknad for etterspurt vedlegg" }
        }
    }

    private fun leggTilEtterspurtVedlegg(soknadsKvittering: SoknadsKvittering, event: SoknadEvent.VedleggEtterspurt) {

        if (soknadsKvittering.etterspurteVedlegg.any { it.vedleggsId == event.vedleggsId }) {
            log.warn { "Kan ikke etterspørre vedlegg på nytt" }
            return
        }

        val alleredeMottatt = if (soknadsKvittering.mottatteVedlegg.any { it.vedleggsId == event.vedleggsId } ) {
            log.info { "Fant eksisterende vedlegg. Markerer som allerede mottatt." }
            true
        } else {
            false
        }

        val vedlegg = EtterspurtVedlegg(
            vedleggsId = event.vedleggsId,
            brukerErAvsender = event.brukerErAvsender,
            tittel = event.tittel,
            beskrivelse = event.beskrivelse,
            linkEttersending = event.linkEttersending,
            tidspunktEtterspurt = event.tidspunktEtterspurt,
            erMottatt = alleredeMottatt
        )

        repository.oppdaterEtterspurteVedlegg(soknadsId = soknadsKvittering.soknadsId, soknadsKvittering.etterspurteVedlegg + vedlegg)

        historikkAppender.vedleggEtterspurt(event)
    }
}
