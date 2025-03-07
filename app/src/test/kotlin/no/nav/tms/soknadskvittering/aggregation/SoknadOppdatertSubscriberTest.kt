package no.nav.tms.soknadskvittering.aggregation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.historikk.HistorikkRepository
import no.nav.tms.soknadskvittering.historikk.firstHistorikkEntry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SoknadOppdatertSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val appender = HistorikkAppender(HistorikkRepository(database))

    private val messageBroadcaster = MessageBroadcaster(
        SoknadInnsendtSubscriber(repository, appender),
        SoknadOppdatertSubscriber(repository, appender)
    )

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadskvittering") }
        database.update { queryOf("delete from soknadsevent_historikk") }
    }

    @Test
    fun `tillater oppdatering av bestemte felt`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val originalFristEttersending = LocalDate.now().plusDays(7)
        val originalLinkSoknad = null
        val originalJournalpostId = null

        innsendtEvent(
            soknadsId,
            ident,
            fristEttersending = originalFristEttersending,
            linkSoknad = originalLinkSoknad,
            journalpostId = originalJournalpostId
        ).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId).let {
            it.shouldNotBeNull()
            it.fristEttersending shouldBe originalFristEttersending
            it.linkSoknad shouldBe originalLinkSoknad
            it.journalpostId shouldBe originalJournalpostId
        }

        val oppdatertFristEttersending = LocalDate.now().plusDays(14)
        val oppdatertLinkSoknad =  "https://lenke.til.soknad"
        val oppdatertJournalpostId = "journalpostId"

        oppdatertEvent(
            soknadsId,
            fristEttersending = oppdatertFristEttersending,
            linkSoknad = oppdatertLinkSoknad,
            journalpostId = oppdatertJournalpostId
        ).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId).let {
            it.shouldNotBeNull()
            it.fristEttersending shouldBe oppdatertFristEttersending
            it.linkSoknad shouldBe oppdatertLinkSoknad
            it.journalpostId shouldBe oppdatertJournalpostId
        }
    }

    @Test
    fun `overskriver ikke eksisterende felt med null`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val endeligLink = "https://en.link"
        val endeligJournalpostId = "journalpost-1"

        innsendtEvent(
            soknadsId,
            ident,
            linkSoknad = null,
            journalpostId = null
        ).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId).let {
            it.shouldNotBeNull()
            it.linkSoknad shouldBe null
            it.journalpostId shouldBe null
        }

        oppdatertEvent(
            soknadsId,
            linkSoknad = endeligLink,
            journalpostId = null
        ).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId).let {
            it.shouldNotBeNull()
            it.linkSoknad shouldBe endeligLink
            it.journalpostId shouldBe null
        }

        oppdatertEvent(
            soknadsId,
            linkSoknad = null,
            journalpostId = endeligJournalpostId
        ).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId).let {
            it.shouldNotBeNull()
            it.linkSoknad shouldBe endeligLink
            it.journalpostId shouldBe endeligJournalpostId
        }
    }

    @Test
    fun `ignorerer oppdatering for ukjente soknadsIder`() {
        val soknadsId = UUID.randomUUID().toString()

        oppdatertEvent(soknadsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        database.single {
            queryOf("select count(*) as antall from soknadskvittering")
                .map { it.int("antall") }.asSingle
        } shouldBe 0
    }

    @Test
    fun `legger til hendelse i event-historikk når søknad blir oppdatert`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let { messageBroadcaster.broadcastJson(it) }
        oppdatertEvent(soknadsId, journalpostId = "ny-123").let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "soknadOppdatert").shouldNotBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når søknad som skulle oppdateres ikke finnes`() {
        val soknadsId = UUID.randomUUID().toString()

        oppdatertEvent(soknadsId, journalpostId = "ny-123").let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "soknadOppdatert").shouldBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når oppdatert-event ikke hadde noe innhold`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let { messageBroadcaster.broadcastJson(it) }
        oppdatertEvent(
            soknadsId,
            fristEttersending = null,
            journalpostId = null,
            linkSoknad = null
        ).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "soknadOppdatert").shouldBeNull()
    }
}
