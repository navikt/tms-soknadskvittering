package no.nav.tms.soknadskvittering.aggregation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.common.shouldBeSameTimeAs
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.historikk.HistorikkRepository
import no.nav.tms.soknadskvittering.historikk.firstHistorikkEntry
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import org.junit.jupiter.api.Test
import java.util.*

class VedleggMottattSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val appender = HistorikkAppender(HistorikkRepository(database))

    private val messageBroadcaster = MessageBroadcaster(
        SoknadInnsendtSubscriber(repository, appender),
        VedleggMottattSubscriber(repository, appender)
    )

    @Test
    fun `legger til mottatte vedlegg for soknad`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        innsendtEvent(soknadsId, ident).let {
            messageBroadcaster.broadcastJson(it)
        }

        val vedleggsId = "vedlegg-1"
        val tittel = "Vedlegg om ett eller annet"
        val linkVedlegg = "https://link.til.vedlegg"
        val journalpostId = "789456"
        val brukerErAvsender = true
        val tidspunktMottatt = ZonedDateTimeHelper.nowAtUtc()


        vedleggMottattEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            tittel = tittel,
            linkVedlegg = linkVedlegg,
            journalpostId = journalpostId,
            brukerErAvsender = brukerErAvsender,
            tidspunktMottatt = tidspunktMottatt
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)!!

        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.tittel shouldBe tittel
            it.linkVedlegg shouldBe linkVedlegg
            it.journalpostId shouldBe journalpostId
            it.brukerErAvsender shouldBe brukerErAvsender
            it.tidspunktMottatt shouldBe tidspunktMottatt
        }
    }

    @Test
    fun `markerer etterspurt vedlegg som mottatt`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        innsendtEvent(
            soknadsId,
            ident,
            etterspurteVedlegg = listOf(
                etterspurtVedleggJson(vedleggsId = vedleggsId)
            )
        ).let { messageBroadcaster.broadcastJson(it) }

        vedleggMottattEvent(soknadsId, vedleggsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        val kvittering = repository.getSoknadsKvittering(soknadsId)!!

        kvittering.mottatteVedlegg.size shouldBe 1
        kvittering.etterspurteVedlegg.size shouldBe 1

        kvittering.etterspurteVedlegg.first().erMottatt shouldBe true
    }

    @Test
    fun `tillater ikke å motta vedlegg som allerede er mottatt`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val mottatt = ZonedDateTimeHelper.nowAtUtc().plusHours(1)

        innsendtEvent(
            soknadsId,
            ident,
            mottatteVedlegg = listOf(
                mottattVedleggJson(vedleggsId = vedleggsId)
            )
        ).let { messageBroadcaster.broadcastJson(it) }

        vedleggMottattEvent(soknadsId, vedleggsId, tidspunktMottatt = mottatt).let {
            messageBroadcaster.broadcastJson(it)
        }

        repository.getSoknadsKvittering(soknadsId)!!.let {
            it.mottatteVedlegg.size shouldBe 1

            it.mottatteVedlegg.first().tidspunktMottatt shouldBeSameTimeAs it.tidspunktMottatt
        }
    }

    @Test
    fun `ignorerer mottatte vedlegg som ikke tilhører en kjent soknad`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()

        vedleggMottattEvent(soknadsId, vedleggsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        database.single {
            queryOf("select count(*) as antall from soknadskvittering")
                .map { it.int("antall") }.asSingle
        } shouldBe 0
    }

    @Test
    fun `legger til hendelse i event-historikk når vedlegg blir mottatt`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "nytt-vedlegg"
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let { messageBroadcaster.broadcastJson(it) }
        vedleggMottattEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggMottatt").shouldNotBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når det allerede er mottatt`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"
        val ident = "12345678900"

        val vedlegg = mottattVedleggJson(vedleggsId)

        opprettetEvent(soknadsId, ident, mottatteVedlegg = listOf(vedlegg)).let { messageBroadcaster.broadcastJson(it) }
        vedleggMottattEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggMottatt").shouldBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når tilhørende søknad ikke finnes`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "nytt-vedlegg"

        vedleggMottattEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggMottatt").shouldBeNull()
    }
}
