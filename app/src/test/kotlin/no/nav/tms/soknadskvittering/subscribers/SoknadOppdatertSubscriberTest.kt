package no.nav.tms.soknadskvittering.subscribers

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SoknadOppdatertSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()

    private val repository = SoknadsKvitteringRepository(database)

    private val messageBroadcaster = MessageBroadcaster(
        SoknadOpprettetSubscriber(repository),
        SoknadOppdatertSubscriber(repository)
    )

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadskvittering") }
    }

    @Test
    fun `tillater oppdatering av bestemte felt`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val originalFristEttersending = LocalDate.now().plusDays(7)
        val originalLinkSoknad = null
        val originalJournalpostId = null

        opprettetEvent(
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

        opprettetEvent(
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
}
