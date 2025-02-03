package no.nav.tms.soknadskvittering.subscribers

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.SoknadsKvittering
import no.nav.tms.soknadskvittering.MottattVedlegg
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.asZonedDateTime
import no.nav.tms.soknadskvittering.setup.withMDC

class VedleggMottattSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedlegg_mottatt")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "tidspunktMottatt",
            "brukerErAvsender",
            "tittel"
        )
        .withOptionalFields(
            "linkVedlegg"
        )

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val soknadsId = jsonMessage["soknadsId"].asText()

        val soknadsKvittering = repository.getSoknadsKvittering(soknadsId)

        if (soknadsKvittering == null) {
            log.info { "Fant ikke soknad for mottatt vedlegg" }
            return@withMDC
        }

        val vedleggsId = jsonMessage["vedleggsId"].asText()

        if (soknadsKvittering.mottatteVedlegg.any { it.vedleggsId == vedleggsId}) {
            log.info { "Kan ikke legge til samme vedlegg to ganger" }

        } else if (soknadsKvittering.etterspurteVedlegg.any { it.vedleggsId == vedleggsId }) {
            log.info { "Legger til mottatt vedlegg for etterspørsel" }

            oppdaterVedlegg(soknadsKvittering, jsonMessage)
        } else {
            log.info { "Legger til mottatt vedlegg" }

            nyttVedlegg(soknadsKvittering, jsonMessage)
        }
    }


    private fun oppdaterVedlegg(
        soknadsKvittering: SoknadsKvittering,
        jsonMessage: JsonMessage
    ) {
        val vedleggsId = jsonMessage["vedleggsId"].asText()

        val mottatteVedlegg = soknadsKvittering.mottatteVedlegg + MottattVedlegg(
            vedleggsId = vedleggsId,
            brukerErAvsender = jsonMessage["brukerErAvsender"].asBoolean(),
            tittel = jsonMessage["tittel"].asText(),
            linkVedlegg = jsonMessage.getOrNull("linkVedlegg")?.asText(),
            tidspunktMottatt = jsonMessage["tidspunktMottatt"].asZonedDateTime()
        )

        val etterspurteVedlegg = soknadsKvittering.etterspurteVedlegg.filter { it.vedleggsId != vedleggsId }

        repository.oppdaterAlleVedlegg(soknadsKvittering.soknadsId, mottatteVedlegg, etterspurteVedlegg)
    }

    private fun nyttVedlegg(soknadsKvittering: SoknadsKvittering, jsonMessage: JsonMessage) {
        MottattVedlegg(
            vedleggsId = jsonMessage["vedleggsId"].asText(),
            brukerErAvsender = jsonMessage["brukerErAvsender"].asBoolean(),
            tittel = jsonMessage["tittel"].asText(),
            linkVedlegg = jsonMessage.getOrNull("linkVedlegg")?.asText(),
            tidspunktMottatt = jsonMessage["ttidspunktMottatt"].asZonedDateTime()
        ).let {
            repository.oppdaterMottatteVedlegg(soknadsKvittering.soknadsId, soknadsKvittering.mottatteVedlegg + it)
        }
    }
}
