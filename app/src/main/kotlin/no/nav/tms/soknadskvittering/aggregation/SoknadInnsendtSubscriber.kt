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

class SoknadInnsendtSubscriber(
    private val repository: SoknadsKvitteringRepository,
    private val historikkAppender: HistorikkAppender
): Subscriber() {

    override fun subscribe() = Subscription.forEvents("soknadOpprettet", "soknadInnsendt")
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
        val innsendtEvent: SoknadEvent.SoknadInnsendt = objectMapper.treeToValue(jsonMessage.json)

        val mottatteVedlegg = innsendtEvent.mottatteVedlegg.map {
            MottattVedlegg(
                erEttersending = false,
                vedleggsId = it.vedleggsId,
                tittel = it.tittel,
                linkVedlegg = it.linkVedlegg,
                journalpostId = it.journalpostId,
                brukerErAvsender = true,
                tidspunktMottatt = innsendtEvent.tidspunktMottatt
            )
        }

        val etterspurteVedlegg = innsendtEvent.etterspurteVedlegg.map {
            EtterspurtVedlegg(
                vedleggsId = it.vedleggsId,
                brukerErAvsender = it.brukerErAvsender,
                tittel = it.tittel,
                linkEttersending = it.linkEttersending,
                beskrivelse = it.beskrivelse,
                tidspunktEtterspurt = innsendtEvent.tidspunktMottatt,
                erMottatt = false
            )
        }

        val soknadsKvittering = SoknadsKvittering(
            soknadsId = innsendtEvent.soknadsId,
            ident = innsendtEvent.ident,
            tittel = innsendtEvent.tittel,
            temakode = innsendtEvent.temakode,
            skjemanummer = innsendtEvent.skjemanummer,
            tidspunktMottatt = innsendtEvent.tidspunktMottatt,
            fristEttersending = innsendtEvent.fristEttersending,
            linkSoknad = innsendtEvent.linkSoknad,
            journalpostId = innsendtEvent.journalpostId,
            mottatteVedlegg = mottatteVedlegg,
            etterspurteVedlegg = etterspurteVedlegg,
            produsent = Produsent(
                cluster = innsendtEvent.produsent.cluster,
                namespace = innsendtEvent.produsent.namespace,
                appnavn = innsendtEvent.produsent.appnavn
            ),
            opprettet = ZonedDateTimeHelper.nowAtUtc(),
            ferdigstilt = null
        )

        repository.insertSoknadsKvittering(soknadsKvittering).let { wasCreated ->
            if (wasCreated) {
                historikkAppender.soknadOpprettet(opprettetEvent)
                log.info { "Opprettet kvittering for innsendt søknad" }
            } else {
                log.warn { "Ignorerte duplikat soknadskvittering" }
            }
        }
    }
}
