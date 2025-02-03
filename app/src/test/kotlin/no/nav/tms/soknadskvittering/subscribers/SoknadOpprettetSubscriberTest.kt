package no.nav.tms.soknadskvittering.subscribers

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.common.shouldBeSameTimeAs
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.nowAtUtc
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SoknadOpprettetSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()

    private val repository = SoknadsKvitteringRepository(database)

    private val messageBroadcaster = MessageBroadcaster(SoknadOpprettetSubscriber(repository))

    @Test
    fun `oppretter søknadskvittering i databasen`() {

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"
        val tittel = "En søknad"
        val temakode = "AAP"
        val skjemanummer = "skjema-123"
        val tidspunktMottatt = nowAtUtc().minusMinutes(1)
        val fristEttersending = LocalDate.now().plusDays(14)
        val linkSoknad = "https://link.til.soknad"
        val journalpostId = "123"

        val mottattVedleggVedleggsId = "vedlegg-1"
        val mottattVedleggTittel = "Ett eller annet dokument"
        val mottattVedleggLinkVedlegg = "http://dokument-db/brukers-fil.pdf"

        val etterspurtVedleggVedleggsId = "vedlegg-2"
        val etterspurtVedleggBrukerErAvsender = false
        val etterspurtVedleggTittel = "Noen andre må gjøre noe"
        val etterspurtVedleggBeskrivelse = "Noen andre må gjøre noe på dine vegne. Bla bla bla.."
        val etterspurtVedleggLinkEttersending = null

        val event = opprettetEvent(
            soknadsId = soknadsId,
            ident = ident,
            tittel = tittel,
            temakode = temakode,
            skjemanummer = skjemanummer,
            tidspunktMottatt = tidspunktMottatt,
            fristEttersending = fristEttersending,
            linkSoknad = linkSoknad,
            journalpostId = journalpostId,
            mottatteVedlegg = listOf(
                mottattVedleggJson(
                    vedleggsId = mottattVedleggVedleggsId,
                    tittel = mottattVedleggTittel,
                    linkVedlegg = mottattVedleggLinkVedlegg
                )
            ),
            etterspurteVedlegg = listOf(
                etterspurteVedleggJson(
                    vedleggsId = etterspurtVedleggVedleggsId,
                    brukerErAvsender = etterspurtVedleggBrukerErAvsender,
                    tittel = etterspurtVedleggTittel,
                    beskrivelse = etterspurtVedleggBeskrivelse,
                    linkEttersending = etterspurtVedleggLinkEttersending
                )
            ),
        )

        messageBroadcaster.broadcastJson(event)

        val kvittering = repository.getSoknadsKvittering(soknadsId)
        kvittering.shouldNotBeNull()

        kvittering.let {
            it.soknadsId shouldBe soknadsId
            it.ident shouldBe ident
            it.tittel shouldBe tittel
            it.temakode shouldBe temakode
            it.skjemanummer shouldBe skjemanummer
            it.tidspunktMottatt shouldBeSameTimeAs tidspunktMottatt
            it.fristEttersending shouldBe fristEttersending
            it.linkSoknad shouldBe linkSoknad
            it.journalpostId shouldBe journalpostId
        }

        kvittering.mottatteVedlegg.size shouldBe 1
        kvittering.mottatteVedlegg.first().let {
            it.vedleggsId shouldBe mottattVedleggVedleggsId
            it.brukerErAvsender shouldBe true
            it.tittel shouldBe mottattVedleggTittel
            it.linkVedlegg shouldBe mottattVedleggLinkVedlegg
            it.tidspunktMottatt shouldBeSameTimeAs tidspunktMottatt
        }

        kvittering.etterspurteVedlegg.size shouldBe 1
        kvittering.etterspurteVedlegg.first().let {
            it.vedleggsId shouldBe etterspurtVedleggVedleggsId
            it.brukerErAvsender shouldBe etterspurtVedleggBrukerErAvsender
            it.tittel shouldBe etterspurtVedleggTittel
            it.linkEttersending shouldBe etterspurtVedleggLinkEttersending
            it.beskrivelse shouldBe etterspurtVedleggBeskrivelse
            it.tidspunktEtterspurt shouldBeSameTimeAs tidspunktMottatt
        }
    }

    @Test
    fun `ignorerer søknader med duplikat id`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val event1 = opprettetEvent(soknadsId, ident, tittel = "En soknad")
        val event2 = opprettetEvent(soknadsId, ident, tittel = "En annen soknad")

        messageBroadcaster.broadcastJson(event1)
        messageBroadcaster.broadcastJson(event2)

        repository.getSoknadsKvittering(soknadsId).shouldNotBeNull()
        repository.getSoknadskvitteringForUser(ident).size shouldBe 1
    }
}
