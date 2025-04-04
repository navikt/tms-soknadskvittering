package no.nav.tms.soknadskvittering.historikk

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto
import no.nav.tms.soknadskvittering.aggregation.SoknadsKvitteringRepository
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.asZonedDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class HistorikkAppenderTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = HistorikkRepository(database)

    private val appender = HistorikkAppender(repository)

    private val soknadsId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        insertUnderlyingSoknadsKvittering()
    }

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadsevent_historikk") }
    }

    @Test
    fun `legger til innsendt-event på ventet format`() {
        val ident = "12345678900"
        val tittel = "En søknad"
        val temakode = "AAP"
        val skjemanummer = "skjema-123"
        val tidspunktMottatt = ZonedDateTimeHelper.nowAtUtc().minusMinutes(1)
        val fristEttersending = LocalDate.now().plusDays(14)
        val linkSoknad = "https://link.til.soknad"
        val linkEttersending = "https://link.til.ettersending"
        val journalpostId = "123"

        val mottattVedleggVedleggsId = "vedlegg-1"
        val mottattVedleggTittel = "Ett eller annet dokument"
        val mottattVedleggLinkVedlegg = "http://dokument-db/brukers-fil.pdf"
        val mottattVedleggJournalpostId = "456"

        val etterspurtVedleggVedleggsId = "vedlegg-2"
        val etterspurtVedleggBrukerErAvsender = false
        val etterspurtVedleggTittel = "Noen andre må gjøre noe"
        val etterspurtVedleggBeskrivelse = "Noen andre må gjøre noe på dine vegne. Bla bla bla.."
        val etterspurtVedleggLinkEttersending = null

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadInnsendt(
            soknadsId = soknadsId,
            ident = ident,
            tittel = tittel,
            temakode = temakode,
            skjemanummer = skjemanummer,
            tidspunktMottatt = tidspunktMottatt,
            fristEttersending = fristEttersending,
            linkSoknad = linkSoknad,
            linkEttersending = linkEttersending,
            journalpostId = journalpostId,
            mottatteVedlegg = listOf(
                SoknadEvent.Dto.MottattVedlegg(
                    vedleggsId = mottattVedleggVedleggsId,
                    tittel = mottattVedleggTittel,
                    linkVedlegg = mottattVedleggLinkVedlegg,
                    journalpostId = mottattVedleggJournalpostId
                )
            ),
            etterspurteVedlegg = listOf(
                SoknadEvent.Dto.EtterspurtVedlegg(
                    vedleggsId = etterspurtVedleggVedleggsId,
                    brukerErAvsender = etterspurtVedleggBrukerErAvsender,
                    tittel = etterspurtVedleggTittel,
                    beskrivelse = etterspurtVedleggBeskrivelse,
                    linkEttersending = etterspurtVedleggLinkEttersending
                )
            ),
            produsent = produsent,
            metadata = null
        ).let { appender.soknadInnsendt(it) }

        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "soknadInnsendt"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["ident"].asText() shouldBe ident
            innhold["tittel"].asText() shouldBe tittel
            innhold["temakode"].asText() shouldBe temakode
            innhold["skjemanummer"].asText() shouldBe skjemanummer
            innhold["tidspunktMottatt"].asZonedDateTime() shouldBe tidspunktMottatt
            innhold["fristEttersending"].asText() shouldBe fristEttersending.toString()
            innhold["linkSoknad"].asText() shouldBe linkSoknad
            innhold["journalpostId"].asText() shouldBe journalpostId

            innhold["mottatteVedlegg"].first().let { vedlegg ->
                vedlegg["vedleggsId"].asText() shouldBe mottattVedleggVedleggsId
                vedlegg["tittel"].asText() shouldBe mottattVedleggTittel
                vedlegg["linkVedlegg"].asText() shouldBe mottattVedleggLinkVedlegg
            }

            innhold["etterspurteVedlegg"].first().let { vedlegg ->
                vedlegg["vedleggsId"].asText() shouldBe etterspurtVedleggVedleggsId
                vedlegg["brukerErAvsender"].asBoolean() shouldBe etterspurtVedleggBrukerErAvsender
                vedlegg["tittel"].asText() shouldBe etterspurtVedleggTittel
                vedlegg["linkEttersending"]?.asText() shouldBe etterspurtVedleggLinkEttersending
                vedlegg["beskrivelse"]?.asText() shouldBe etterspurtVedleggBeskrivelse
            }
        }
    }

    @Test
    fun `legger til oppdatert-event på ventet format`() {
        val linkSoknad = "https://annen.link.til.soknad"
        val journalpostId = "456"

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadOppdatert(
            soknadsId = soknadsId,
            fristEttersending = null,
            linkSoknad = linkSoknad,
            journalpostId = journalpostId,
            produsent = produsent,
            metadata = null
        ).let { appender.soknadOppdatert(it) }

        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "soknadOppdatert"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["fristEttersending"]?.asText() shouldBe null
            innhold["linkSoknad"].asText() shouldBe linkSoknad
            innhold["journalpostId"].asText() shouldBe journalpostId
        }
    }

    @Test
    fun `legger til ferdigstilt-event på ventet format`() {

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadFerdigstilt(
            soknadsId = soknadsId,
            produsent = produsent,
            metadata = null
        ).let { appender.soknadFerdigstilt(it) }

        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "soknadFerdigstilt"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.shouldBeNull()
    }

    @Test
    fun `legger til vedleggEtterspurt-event på ventet format`() {

        val vedleggsId = "vedlegg-1"
        val brukerErAvsender = true
        val tittel = "Tittel på vedlegg som skal sendes av bruker"
        val linkEttersending = "https://link.til.ettersending"

        val tidspunkt = ZonedDateTimeHelper.nowAtUtc().minusDays(1)

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.VedleggEtterspurt(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            brukerErAvsender = brukerErAvsender,
            tittel = tittel,
            linkEttersending = linkEttersending,
            beskrivelse = null,
            tidspunktEtterspurt = tidspunkt,
            produsent = produsent,
            metadata = null,
        ).let { appender.vedleggEtterspurt(it) }


        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "vedleggEtterspurt"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["vedleggsId"].asText() shouldBe vedleggsId
            innhold["brukerErAvsender"].asBoolean() shouldBe brukerErAvsender
            innhold["tittel"].asText() shouldBe tittel
            innhold["linkEttersending"].asText() shouldBe linkEttersending
            innhold["beskrivelse"]?.asText() shouldBe null
            innhold["tidspunktEtterspurt"].asZonedDateTime() shouldBe tidspunkt
        }

    }

    @Test
    fun `legger til vedleggMottatt-event på ventet format`() {

        val vedleggsId = "vedlegg-2"
        val brukerErAvsender = true
        val tittel = "Tittel på vedlegg som er mottatt"
        val linkVedlegg = "https://link.til.vedlegg"
        val journalpostId = "123456"

        val tidspunkt = ZonedDateTime.parse("2025-03-02T12:00:00Z")

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.VedleggMottatt(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            brukerErAvsender = brukerErAvsender,
            tittel = tittel,
            linkVedlegg = linkVedlegg,
            journalpostId = journalpostId,
            tidspunktMottatt = tidspunkt,
            produsent = produsent,
            metadata = null,
        ).let { appender.vedleggMottatt(it) }


        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "vedleggMottatt"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["vedleggsId"].asText() shouldBe vedleggsId
            innhold["brukerErAvsender"].asBoolean() shouldBe brukerErAvsender
            innhold["tittel"].asText() shouldBe tittel
            innhold["linkVedlegg"].asText() shouldBe linkVedlegg
            innhold["tidspunktMottatt"].asZonedDateTime() shouldBe tidspunkt
        }

    }

    @Test
    fun `legger til vedleggOppdatert-event på ventet format`() {

        val vedleggsId = "vedlegg-3"
        val linkVedlegg = "https://ny.link.til.vedlegg"
        val journalpostId = "123456"

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.VedleggOppdatert(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkVedlegg = linkVedlegg,
            journalpostId = journalpostId,
            produsent = produsent,
            metadata = null,
        ).let { appender.vedleggOppdatert(it) }


        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "vedleggOppdatert"
        entry.soknadsId shouldBe soknadsId
        entry.produsent shouldBe produsent

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["vedleggsId"].asText() shouldBe vedleggsId
            innhold["linkVedlegg"].asText() shouldBe linkVedlegg
            innhold["journalpostId"].asText() shouldBe journalpostId
        }
    }

    @Test
    fun `ignorer bestemte felt fra eventet i json-objektet i 'innhold'`() {

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadOppdatert(
            soknadsId = soknadsId,
            produsent = produsent,
            fristEttersending = null,
            linkSoknad = null,
            journalpostId = null,
            metadata = mapOf("meta" to "data")
        ).let { appender.soknadOppdatert(it) }

        val entry = database.firstHistorikkEntry(soknadsId)

        entry.shouldNotBeNull()
        entry.event shouldBe "soknadOppdatert"

        entry.innhold.let { innhold ->
            innhold.shouldNotBeNull()
            innhold["eventName"].shouldBeNull()
            innhold["@event_name"].shouldBeNull()

            innhold["soknadsId"].shouldBeNull()
            innhold["produsent"].shouldBeNull()
            innhold["metadata"].shouldBeNull()
        }
    }

    private fun insertUnderlyingSoknadsKvittering() {
        DatabaseDto.SoknadsKvittering(
            soknadsId = soknadsId,
            ident = "dummy",
            tittel = "dummy",
            temakode = "dummy",
            skjemanummer = "dummy",
            tidspunktMottatt = ZonedDateTime.now(),
            fristEttersending = LocalDate.now(),
            linkSoknad = "https://dummy",
            linkEttersending = "https://dummy",
            journalpostId = "dummy",
            mottatteVedlegg = emptyList(),
            etterspurteVedlegg = emptyList(),
            produsent = DatabaseDto.Produsent("", "", ""),
            opprettet = ZonedDateTime.now(),
            ferdigstilt = null,
        ).let {
            SoknadsKvitteringRepository(database).insertSoknadsKvittering(it)
        }
    }
}
