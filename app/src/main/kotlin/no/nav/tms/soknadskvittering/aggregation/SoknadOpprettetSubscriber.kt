package no.nav.tms.soknadskvittering.aggregation

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.*
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.SoknadsKvittering
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.MottattVedlegg
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.Produsent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadOpprettetSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknadOpprettet")
        .withFields(
            "soknadsId",
            "ident",
            "tittel",
            "temakode",
            "skjemanummer",
            "tidspunktMottatt",
            "fristEttersending",
            "mottatteVedlegg",
            "etterspurteVedlegg",
            "produsent"
        )
        .withOptionalFields(
            "linkSoknad",
            "journalpostId",
            "metadata"
        )

    private val log = KotlinLogging.logger {}

    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val opprettetEvent: SoknadEvent.SoknadOpprettet = objectMapper.treeToValue(jsonMessage.json)

        val mottatteVedlegg = opprettetEvent.mottatteVedlegg.map {
            MottattVedlegg(
                erEttersending = false,
                vedleggsId = it.vedleggsId,
                tittel = it.tittel,
                linkVedlegg = it.linkVedlegg,
                brukerErAvsender = true,
                tidspunktMottatt = opprettetEvent.tidspunktMottatt
            )
        }

        val etterspurteVedlegg = opprettetEvent.etterspurteVedlegg.map {
            EtterspurtVedlegg(
                vedleggsId = it.vedleggsId,
                brukerErAvsender = it.brukerErAvsender,
                tittel = it.tittel,
                linkEttersending = it.linkEttersending,
                beskrivelse = it.beskrivelse,
                tidspunktEtterspurt = opprettetEvent.tidspunktMottatt,
                erMottatt = false
            )
        }

        val soknadsKvittering = SoknadsKvittering(
            soknadsId = opprettetEvent.soknadsId,
            ident = opprettetEvent.ident,
            tittel = opprettetEvent.tittel,
            temakode = opprettetEvent.temakode,
            skjemanummer = opprettetEvent.skjemanummer,
            tidspunktMottatt = opprettetEvent.tidspunktMottatt,
            fristEttersending = opprettetEvent.fristEttersending,
            linkSoknad = opprettetEvent.linkSoknad,
            journalpostId = opprettetEvent.journalpostId,
            mottatteVedlegg = mottatteVedlegg,
            etterspurteVedlegg = etterspurteVedlegg,
            produsent = Produsent(
                cluster = opprettetEvent.produsent.cluster,
                namespace = opprettetEvent.produsent.namespace,
                appnavn = opprettetEvent.produsent.appnavn
            ),
            opprettet = ZonedDateTimeHelper.nowAtUtc(),
            ferdigstilt = null
        )

        repository.insertSoknadsKvittering(soknadsKvittering).let { wasCreated ->
            if (wasCreated) {
                historikkAppender.soknadOpprettet(opprettetEvent)
                log.info { "Opprettet ny soknadskvittering" }
            } else {
                log.warn { "Ignorerte duplikat soknadskvittering" }
            }
        }
    }
}
