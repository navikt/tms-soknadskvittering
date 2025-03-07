package no.nav.tms.soknadskvittering.aggregation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.historikk.HistorikkRepository
import no.nav.tms.soknadskvittering.historikk.firstHistorikkEntry
import org.junit.jupiter.api.Test
import java.util.*

class VedleggOppdatertSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val appender = HistorikkAppender(HistorikkRepository(database))

    private val messageBroadcaster = MessageBroadcaster(
        SoknadInnsendtSubscriber(repository, appender),
        VedleggOppdatertSubscriber(repository, appender)
    )

    @Test
    fun `oppdaterer vedlegg for soknad`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val vedleggsId = "vedlegg-1"

        val vedleggJson = mottattVedleggJson(vedleggsId = vedleggsId, linkVedlegg = null)

        innsendtEvent(soknadsId, ident = ident, mottatteVedlegg = listOf(vedleggJson)).let {
            messageBroadcaster.broadcastJson(it)
        }

        val linkVedlegg = "https://ferdig.link.til.vedlegg"

        vedleggOppdatertEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkVedlegg = linkVedlegg
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.linkVedlegg.shouldNotBeNull()
            it.linkVedlegg shouldBe linkVedlegg
        }
    }

    @Test
    fun `tillater overskriving av eksisterende data med annen data`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val vedleggsId = "vedlegg-1"
        val gammelLink = "https://gammel.link.til.vedlegg"
        val gammelJournallpostId = "123456"

        val vedleggJson = mottattVedleggJson(
            vedleggsId = vedleggsId, linkVedlegg = gammelLink, journalpostId =  gammelJournallpostId
        )

        innsendtEvent(soknadsId, ident = ident, mottatteVedlegg = listOf(vedleggJson)).let {
            messageBroadcaster.broadcastJson(it)
        }

        val nyLink = "https://ny.link.til.vedlegg"
        val nyJournalpostId = "789456"

        vedleggOppdatertEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkVedlegg = nyLink,
            journalpostId = nyJournalpostId

        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.linkVedlegg shouldNotBe gammelLink
            it.linkVedlegg shouldBe nyLink
            it.journalpostId shouldNotBe gammelJournallpostId
            it.journalpostId shouldBe nyJournalpostId
        }
    }

    @Test
    fun `tillater ikke overskriving av eksisterende lenke med null`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val vedleggsId = "vedlegg-1"
        val gammelLink = "https://gammel.link.til.vedlegg"

        val vedleggJson = mottattVedleggJson(vedleggsId = vedleggsId, linkVedlegg = gammelLink)

        innsendtEvent(soknadsId, ident = ident, mottatteVedlegg = listOf(vedleggJson)).let {
            messageBroadcaster.broadcastJson(it)
        }

        val nyLink = null

        vedleggOppdatertEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkVedlegg = nyLink
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.linkVedlegg.shouldNotBeNull()
            it.linkVedlegg shouldBe gammelLink
        }
    }

    @Test
    fun `tillater ikke oppdatering av etterspurt vedlegg`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val vedleggsId = "vedlegg-1"
        val gammelLink = "https://gammel.link.til.ettersending"

        val vedleggJson = etterspurtVedleggJson(vedleggsId = vedleggsId, linkEttersending = gammelLink)

        innsendtEvent(soknadsId, ident = ident, etterspurteVedlegg = listOf(vedleggJson)).let {
            messageBroadcaster.broadcastJson(it)
        }

        val nyLink = "https://annen.link.til.ettersending"

        vedleggEtterspurtEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            linkEttersending = nyLink
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.mottatteVedlegg.size shouldBe 0
        kvittering.etterspurteVedlegg.size shouldBe 1

        kvittering.etterspurteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.linkEttersending.shouldNotBeNull()
            it.linkEttersending shouldBe gammelLink
        }
    }

    @Test
    fun `ignorerer oppdaterte vedlegg som ikke tilhører en kjent soknad`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()

        vedleggOppdatertEvent(soknadsId, vedleggsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        database.single {
            queryOf("select count(*) as antall from soknadskvittering")
                .map { it.int("antall") }.asSingle
        } shouldBe 0
    }

    @Test
    fun `ignorerer oppdateringer som ikke tilhører et kjent vedlegg`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val vedleggsId1 = "vedlegg-1"
        val vedleggsId2 = "vedlegg-2"


        val vedleggJson = mottattVedleggJson(vedleggsId = vedleggsId1, linkVedlegg = null)

        innsendtEvent(soknadsId, ident = ident, mottatteVedlegg = listOf(vedleggJson)).let {
            messageBroadcaster.broadcastJson(it)
        }

        val linkVedlegg = "https://ferdig.link.til.vedlegg"

        vedleggOppdatertEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId2,
            linkVedlegg = linkVedlegg
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId1 }.let {
            it.linkVedlegg.shouldBeNull()
        }
    }

    @Test
    fun `legger til hendelse i event-historikk når vedlegg blir oppdatert`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"
        val ident = "12345678900"

        val vedlegg = mottattVedleggJson(vedleggsId, linkVedlegg = null)

        opprettetEvent(soknadsId, ident, mottatteVedlegg = listOf(vedlegg)).let { messageBroadcaster.broadcastJson(it) }
        vedleggOppdatertEvent(soknadsId, vedleggsId, linkVedlegg = "https://ny.link").let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggOppdatert").shouldNotBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når oppdatert-event ikke har noe innhold`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"
        val ident = "12345678900"

        val vedlegg = mottattVedleggJson(vedleggsId)

        opprettetEvent(soknadsId, ident, mottatteVedlegg = listOf(vedlegg)).let { messageBroadcaster.broadcastJson(it) }
        vedleggOppdatertEvent(soknadsId, vedleggsId, linkVedlegg = null).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggOppdatert").shouldBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når vedlegg ikke finnes`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "finnesikke"
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident, mottatteVedlegg = emptyList()).let { messageBroadcaster.broadcastJson(it) }
        vedleggOppdatertEvent(soknadsId, vedleggsId, linkVedlegg = "https://ny.link").let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggOppdatert").shouldBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når tilhørende søknad ikke finnes`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"

        vedleggOppdatertEvent(soknadsId, vedleggsId, linkVedlegg = "https://ny.link").let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggOppdatert").shouldBeNull()
    }
}
