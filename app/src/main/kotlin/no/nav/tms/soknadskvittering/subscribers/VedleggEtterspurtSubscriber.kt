package no.nav.tms.soknadskvittering.subscribers

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.SoknadsKvittering
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.asZonedDateTime
import no.nav.tms.soknadskvittering.setup.withMDC

class VedleggEtterspurtSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedlegg_etterspurt")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "brukerErAvsender",
            "tittel",
            "tidspunktEtterspurt"
        )
        .withOptionalFields(
            "linkEttersending",
            "beskrivelse"
        )

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val soknadsId = jsonMessage["soknadsId"].asText()

        repository.getSoknadsKvittering(soknadsId)?.let {
            leggTilEtterspurtVedlegg(it, jsonMessage)
        } ?: log.info { "Fant ikke soknad for etterspurt vedlegg" }
    }

    private fun leggTilEtterspurtVedlegg(soknadsKvittering: SoknadsKvittering, jsonMessage: JsonMessage) {
        val vedleggsId = jsonMessage["vedleggsId"].asText()

        val vedleggEksisterer = soknadsKvittering.vedlegg.any { it.vedleggsId == vedleggsId }

        if (vedleggEksisterer) {
            log.info { "Kan ikke etterspørre vedlegg på nytt" }
            return
        }

        val vedlegg = EtterspurtVedlegg(
            vedleggsId = jsonMessage["vedleggsId"].asText(),
            brukerErAvsender = jsonMessage["brukerErAvsender"].asBoolean(),
            tittel = jsonMessage["tittel"].asText(),
            beskrivelse = jsonMessage.getOrNull("beskrivelse")?.asText(),
            linkEttersending = jsonMessage.getOrNull("linkEttersending")?.asText(),
            tidspunktEtterspurt = jsonMessage["tidspunktEtterspurt"].asZonedDateTime()
        )

        repository.oppdaterEtterspurteVedlegg(soknadsId = soknadsKvittering.soknadsId, soknadsKvittering.etterspurteVedlegg + vedlegg)
    }
}
