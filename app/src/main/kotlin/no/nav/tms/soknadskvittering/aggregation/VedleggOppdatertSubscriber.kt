package no.nav.tms.soknadskvittering.aggregation

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.MottattVedlegg
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.SoknadsKvittering
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.withMDC

class VedleggOppdatertSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedleggOppdatert")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "produsent"
        )
        .withOptionalFields(
            "linkVedlegg",
            "journalpostId",
            "metadata"
        )

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val oppdatertEvent: SoknadEvent.VedleggOppdatert = objectMapper.treeToValue(jsonMessage.json)

        if (oppdatertEvent.linkVedlegg == null) {
            log.info { "Ignorer vedleggOppdatert-event ettersom ingen endringer ble spesifisert" }
            return@withMDC
        }

        val soknadsKvittering = repository.getSoknadsKvittering(oppdatertEvent.soknadsId)

        if (soknadsKvittering == null) {
            log.warn { "Fant ikke soknad for oppdatert vedlegg" }
            return@withMDC
        }

        val vedlegg = soknadsKvittering.mottatteVedlegg.firstOrNull { it.vedleggsId == oppdatertEvent.vedleggsId }

        if (vedlegg != null) {
            oppdaterVedlegg(soknadsKvittering, vedlegg, oppdatertEvent)

            historikkAppender.vedleggOppdatert(oppdatertEvent)
            log.info { "Oppdaterte vedlegg" }
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
            linkVedlegg = oppdatertEvent.linkVedlegg ?: vedlegg.linkVedlegg,
            journalpostId = oppdatertEvent.journalpostId ?: vedlegg.journalpostId
        )

        val urelaterteVedlegg = soknadsKvittering.mottatteVedlegg.filter { it.vedleggsId != oppdatertEvent.vedleggsId }

        repository.oppdaterMottatteVedlegg(soknadsKvittering.soknadsId, urelaterteVedlegg + oppdatertVedlegg)
    }
}
