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

class VedleggMottattSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvent("vedleggMottatt")
        .withFields(
            "soknadsId",
            "vedleggsId",
            "tidspunktMottatt",
            "brukerErAvsender",
            "tittel",
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
        val mottattEvent: SoknadEvent.VedleggMottatt = objectMapper.treeToValue(jsonMessage.json)

        val soknadsKvittering = repository.getSoknadsKvittering(mottattEvent.soknadsId)

        if (soknadsKvittering == null) {
            log.warn { "Fant ikke soknad for mottatt vedlegg" }
            return@withMDC
        }

        if (soknadsKvittering.mottatteVedlegg.any { it.vedleggsId == mottattEvent.vedleggsId}) {
            log.warn { "Kan ikke legge til samme vedlegg to ganger" }

        } else if (soknadsKvittering.etterspurteVedlegg.any { it.vedleggsId == mottattEvent.vedleggsId }) {
            oppdaterVedlegg(soknadsKvittering, mottattEvent)

            historikkAppender.vedleggMottatt(mottattEvent)
            log.info { "La til mottatt vedlegg for etterspørsel" }
        } else {
            nyttVedlegg(soknadsKvittering, mottattEvent)

            historikkAppender.vedleggMottatt(mottattEvent)
            log.info { "La til mottatt vedlegg" }
        }
    }


    private fun oppdaterVedlegg(
        soknadsKvittering: SoknadsKvittering,
        mottattEvent: SoknadEvent.VedleggMottatt
    ) {
        val mottatteVedlegg = soknadsKvittering.mottatteVedlegg + MottattVedlegg(
            erEttersending = true,
            vedleggsId = mottattEvent.vedleggsId,
            brukerErAvsender = mottattEvent.brukerErAvsender,
            tittel = mottattEvent.tittel,
            linkVedlegg = mottattEvent.linkVedlegg,
            journalpostId = mottattEvent.journalpostId,
            tidspunktMottatt = mottattEvent.tidspunktMottatt
        )

        val urelaterteVedlegg = soknadsKvittering.etterspurteVedlegg.filter { it.vedleggsId != mottattEvent.vedleggsId }

        val markertMottatt = soknadsKvittering.etterspurteVedlegg
            .first { it.vedleggsId == mottattEvent.vedleggsId }
            .copy(erMottatt = true)

        repository.oppdaterAlleVedlegg(soknadsKvittering.soknadsId, mottatteVedlegg, urelaterteVedlegg + markertMottatt)
    }

    private fun nyttVedlegg(soknadsKvittering: SoknadsKvittering, mottattEvent: SoknadEvent.VedleggMottatt) {
        MottattVedlegg(
            erEttersending = true,
            vedleggsId = mottattEvent.vedleggsId,
            brukerErAvsender = mottattEvent.brukerErAvsender,
            tittel = mottattEvent.tittel,
            linkVedlegg = mottattEvent.linkVedlegg,
            journalpostId = mottattEvent.journalpostId,
            tidspunktMottatt = mottattEvent.tidspunktMottatt
        ).let {
            repository.oppdaterMottatteVedlegg(soknadsKvittering.soknadsId, soknadsKvittering.mottatteVedlegg + it)
        }
    }
}
