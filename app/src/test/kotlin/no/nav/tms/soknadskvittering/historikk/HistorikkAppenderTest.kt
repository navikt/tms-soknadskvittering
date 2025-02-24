package no.nav.tms.soknadskvittering.historikk

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.asZonedDateTime
import no.nav.tms.soknadskvittering.setup.json
import no.nav.tms.soknadskvittering.setup.jsonOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class HistorikkAppenderTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = HistorikkRepository(database)

    private val appender = HistorikkAppender(repository)

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadsevent_historikk") }
    }

    @Test
    fun `legger til opprettet-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"
        val tittel = "En søknad"
        val temakode = "AAP"
        val skjemanummer = "skjema-123"
        val tidspunktMottatt = ZonedDateTimeHelper.nowAtUtc().minusMinutes(1)
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

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadOpprettet(
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
                SoknadEvent.Dto.MottattVedlegg(
                    vedleggsId = mottattVedleggVedleggsId,
                    tittel = mottattVedleggTittel,
                    linkVedlegg = mottattVedleggLinkVedlegg
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
        ).let { appender.soknadOpprettet(it) }

        val entries = getEntries(soknadsId)

        entries.size shouldBe 1

        entries.first().let {
            it.event shouldBe "soknadOpprettet"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.let { innhold ->
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
    }

    @Test
    fun `legger til oppdatert-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()
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

        val entries = getEntries(soknadsId)

        entries.size shouldBe 1

        entries.first().let {
            it.event shouldBe "soknadOppdatert"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.let { innhold ->
                innhold.shouldNotBeNull()
                innhold["fristEttersending"]?.asText() shouldBe null
                innhold["linkSoknad"].asText() shouldBe linkSoknad
                innhold["journalpostId"].asText() shouldBe journalpostId
            }
        }
    }

    @Test
    fun `legger til ferdigstilt-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadFerdigstilt(
            soknadsId = soknadsId,
            produsent = produsent,
            metadata = null
        ).let { appender.soknadFerdigstilt(it) }

        val entries = getEntries(soknadsId)

        entries.size shouldBe 1

        entries.first().let {
            it.event shouldBe "soknadFerdigstilt"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.shouldBeNull()
        }
    }

    @Test
    fun `legger til vedleggEtterspurt-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()

        val vedleggsId = "vedlegg-1"
        val brukerErAvsender = true
        val tittel = "Tittel på vedlegg som skal sendes av bruker"
        val linkEttersending = "https://link.til.ettersending"

        val tidspunkt = ZonedDateTime.parse("2025-02-01T12:00:00Z")

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


        val entries = getEntries(soknadsId)

        entries.size shouldBe 1
        entries.first().let {
            it.event shouldBe "vedleggEtterspurt"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.let { innhold ->
                innhold.shouldNotBeNull()
                innhold["vedleggsId"].asText() shouldBe vedleggsId
                innhold["brukerErAvsender"].asText() shouldBe brukerErAvsender
                innhold["tittel"].asText() shouldBe tittel
                innhold["linkEttersending"].asText() shouldBe linkEttersending
                innhold["beskrivelse"]?.asText() shouldBe null
                innhold["tidspunktEtterspurt"].asText() shouldBe tidspunkt
            }
        }
    }

    @Test
    fun `legger til vedleggMottatt-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()

        val vedleggsId = "vedlegg-2"
        val brukerErAvsender = true
        val tittel = "Tittel på vedlegg som er mottatt"
        val linkVedlegg = "https://link.til.vedlegg"

        val tidspunkt = ZonedDateTime.parse("2025-03-02T12:00:00Z")

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.VedleggMottatt(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            brukerErAvsender = brukerErAvsender,
            tittel = tittel,
            linkVedlegg = linkVedlegg,
            tidspunktMottatt = tidspunkt,
            produsent = produsent,
            metadata = null,
        ).let { appender.vedleggMottatt(it) }


        val entries = getEntries(soknadsId)

        entries.size shouldBe 1
        entries.first().let {
            it.event shouldBe "vedleggMottatt"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.let { innhold ->
                innhold.shouldNotBeNull()
                innhold["vedleggsId"].asText() shouldBe vedleggsId
                innhold["brukerErAvsender"].asBoolean() shouldBe brukerErAvsender
                innhold["tittel"].asText() shouldBe tittel
                innhold["linkVedlegg"].asText() shouldBe linkVedlegg
                innhold["tidspunktMottatt"].asZonedDateTime() shouldBe tidspunkt
            }
        }
    }

    @Test
    fun `legger til vedleggOppdatert-event på ventet format`() {
        val soknadsId = UUID.randomUUID().toString()

        val vedleggsId = "vedlegg-3"
        val linkVedlegg = "https://ny.link.til.vedlegg"

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.VedleggOppdatert(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkVedlegg = linkVedlegg,
            produsent = produsent,
            metadata = null,
        ).let { appender.vedleggOppdatert(it) }


        val entries = getEntries(soknadsId)

        entries.size shouldBe 1
        entries.first().let {
            it.event shouldBe "vedleggOppdatert"
            it.soknadsId shouldBe soknadsId
            it.produsent shouldBe produsent

            it.innhold.let { innhold ->
                innhold.shouldNotBeNull()
                innhold["vedleggsId"].asText() shouldBe vedleggsId
                innhold["linkVedlegg"].asText() shouldBe linkVedlegg
            }
        }
    }

    @Test
    fun `ignorer bestemte felt fra eventet i json-objektet i 'innhold'`() {
        val soknadsId = UUID.randomUUID().toString()

        val produsent = SoknadEvent.Dto.Produsent("dev", "team", "app")

        SoknadEvent.SoknadOppdatert(
            soknadsId = soknadsId,
            produsent = produsent,
            fristEttersending = null,
            linkSoknad = null,
            journalpostId = null,
            metadata = mapOf("meta" to "data")
        ).let { appender.soknadOppdatert(it) }

        val entries = getEntries(soknadsId)

        entries.size shouldBe 1

        entries.first().let {
            it.event shouldBe "soknadOppdatert"

            it.innhold.let { innhold ->
                innhold.shouldNotBeNull()
                innhold["eventName"].shouldBeNull()
                innhold["@event_name"].shouldBeNull()

                innhold["soknadsId"].shouldBeNull()
                innhold["produsent"].shouldBeNull()
                innhold["metadata"].shouldBeNull()
            }
        }
    }

    private fun getEntries(soknadsId: String): List<HistorikkEntry> {
        return database.list {
            queryOf(
                "select soknadsId, event, innhold, produsent, tidspunkt from soknadsevent_historikk where soknadsId = :soknadsId",
                mapOf("soknadsId" to soknadsId)
            ).map {
                HistorikkEntry(
                    soknadsId = it.string("soknadsId"),
                    event = it.string("event"),
                    innhold = it.jsonOrNull("innhold"),
                    produsent = it.json("produsent"),
                    tidspunkt = it.zonedDateTime("tidspunkt")
                )
            }.asList
        }
    }
}
