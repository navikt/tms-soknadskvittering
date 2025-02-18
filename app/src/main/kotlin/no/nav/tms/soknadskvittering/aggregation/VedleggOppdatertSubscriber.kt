package no.nav.tms.soknadskvittering.aggregation

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.MottattVedlegg
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.SoknadsKvittering
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.withMDC

class VedleggOppdatertSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedleggOppdatert")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "produsent"
        )
        .withOptionalFields(
            "linkVedlegg",
            "metadata"
        )

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val oppdatertEvent: SoknadEvent.VedleggOppdatert = objectMapper.treeToValue(jsonMessage.json)

        val soknadsKvittering = repository.getSoknadsKvittering(oppdatertEvent.soknadsId)

        if (soknadsKvittering == null) {
            log.warn { "Fant ikke soknad for oppdatert vedlegg" }
            return@withMDC
        }

        val vedlegg = soknadsKvittering.mottatteVedlegg.firstOrNull { it.vedleggsId == oppdatertEvent.vedleggsId }

        if (vedlegg != null) {
            oppdaterVedlegg(soknadsKvittering, vedlegg, oppdatertEvent)
        } else if (soknadsKvittering.etterspurteVedlegg.any { it.vedleggsId == oppdatertEvent.vedleggsId }) {
            log.warn { "Kan ikke oppdatere etterspurt vedlegg" }
        } else {
            log.warn { "Fant ikke vedlegg som skulle oppdateres" }
        }
    }

    private fun oppdaterVedlegg(
        soknadsKvittering: SoknadsKvittering,
        vedlegg: MottattVedlegg,
        oppdatertEvent: SoknadEvent.VedleggOppdatert
    ) {
        val oppdatertVedlegg = vedlegg.copy(
            linkVedlegg = oppdatertEvent.linkVedlegg ?: vedlegg.linkVedlegg
        )

        val urelaterteVedlegg = soknadsKvittering.mottatteVedlegg.filter { it.vedleggsId != oppdatertEvent.vedleggsId }

        repository.oppdaterMottatteVedlegg(soknadsKvittering.soknadsId, urelaterteVedlegg + oppdatertVedlegg)
    }
}
