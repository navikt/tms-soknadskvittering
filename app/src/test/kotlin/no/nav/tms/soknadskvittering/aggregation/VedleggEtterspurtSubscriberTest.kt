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
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.nowAtUtc
import org.junit.jupiter.api.Test
import java.util.*

class VedleggEtterspurtSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val appender = HistorikkAppender(HistorikkRepository(database))

    private val messageBroadcaster = MessageBroadcaster(
        SoknadOpprettetSubscriber(repository, appender),
        VedleggEtterspurtSubscriber(repository, appender)
    )

    @Test
    fun `legger til etterspurt vedlegg for soknad`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let {
            messageBroadcaster.broadcastJson(it)
        }

        val vedleggsId1 = "vedlegg-1"
        val brukerErAvsender1 = true
        val tittel1 = "Tittel på vedlegg som skal sendes av bruker"
        val linkEttersending = "https://link.til.ettersending"

        val vedleggsId2 = "vedlegg-2"
        val brukerErAvsender2 = false
        val tittel2 = "Tittel på vedlegg som skal sendes av annen part"
        val beskrivelse = "Annen part må gjøre noe.."

        val tidspunktEtterspurt = nowAtUtc()

        vedleggEtterspurtEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId1,
            brukerErAvsender = brukerErAvsender1,
            tittel = tittel1,
            linkEttersending = linkEttersending,
            beskrivelse = null,
            tidspunktEtterspurt = tidspunktEtterspurt
        ).let { messageBroadcaster.broadcastJson(it) }

        vedleggEtterspurtEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId2,
            brukerErAvsender = brukerErAvsender2,
            tittel = tittel2,
            linkEttersending = null,
            beskrivelse = beskrivelse,
            tidspunktEtterspurt = tidspunktEtterspurt
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)

        kvittering.shouldNotBeNull()
        kvittering.etterspurteVedlegg.size shouldBe 2

        kvittering.etterspurteVedlegg.first { it.vedleggsId == vedleggsId1 }.let {
            it.brukerErAvsender shouldBe brukerErAvsender1
            it.tittel shouldBe tittel1
            it.linkEttersending shouldBe linkEttersending
            it.beskrivelse shouldBe null
            it.tidspunktEtterspurt shouldBe tidspunktEtterspurt
            it.erMottatt shouldBe false
        }

        kvittering.etterspurteVedlegg.first { it.vedleggsId == vedleggsId2 }.let {
            it.brukerErAvsender shouldBe brukerErAvsender2
            it.tittel shouldBe tittel2
            it.linkEttersending shouldBe null
            it.beskrivelse shouldBe beskrivelse
            it.tidspunktEtterspurt shouldBe tidspunktEtterspurt
            it.erMottatt shouldBe false
        }
    }

    @Test
    fun `tillater ikke å etterspørre vedlegg som allerede er etterspurt`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val etterspurt = nowAtUtc().plusHours(1)

        opprettetEvent(
            soknadsId,
            ident,
            etterspurteVedlegg = listOf(
                etterspurtVedleggJson(vedleggsId = vedleggsId)
            )
        ).let { messageBroadcaster.broadcastJson(it) }

        vedleggEtterspurtEvent(soknadsId, vedleggsId, tidspunktEtterspurt = etterspurt).let {
            messageBroadcaster.broadcastJson(it)
        }

        repository.getSoknadsKvittering(soknadsId)!!.let {
            it.etterspurteVedlegg.size shouldBe 1

            it.etterspurteVedlegg.first().tidspunktEtterspurt shouldBeSameTimeAs it.tidspunktMottatt
        }
    }

    @Test
    fun `markerer som allerede mottatt hvis det finnes et vedlegg for etterspørsel`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(
            soknadsId,
            ident,
            mottatteVedlegg = listOf(
                mottattVedleggJson(vedleggsId = vedleggsId)
            )
        ).let { messageBroadcaster.broadcastJson(it) }

        vedleggEtterspurtEvent(soknadsId, vedleggsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        repository.getSoknadsKvittering(soknadsId)!!.let {
            it.mottatteVedlegg.size shouldBe 1
            it.etterspurteVedlegg.size shouldBe 1

            it.etterspurteVedlegg.first().erMottatt shouldBe true
        }
    }

    @Test
    fun `ignorerer etterspurte vedlegg som ikke tilhører en kjent soknad`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()

        vedleggEtterspurtEvent(soknadsId, vedleggsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        database.single {
            queryOf("select count(*) as antall from soknadskvittering")
                .map { it.int("antall") }.asSingle
        } shouldBe 0
    }

    @Test
    fun `legger til hendelse i event-historikk når vedlegg blir etterspurt`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "nylig-etterspurt"
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let { messageBroadcaster.broadcastJson(it) }
        vedleggEtterspurtEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggEtterspurt").shouldNotBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når vedlegg allerede er etterspurt`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"
        val ident = "12345678900"

        val etterspurt = etterspurtVedleggJson(vedleggsId)

        opprettetEvent(soknadsId, ident, etterspurteVedlegg = listOf(etterspurt)).let { messageBroadcaster.broadcastJson(it) }
        vedleggEtterspurtEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggEtterspurt").shouldBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk når tilhørende søknad ikke finnes`() {
        val soknadsId = UUID.randomUUID().toString()
        val vedleggsId = "vedlegg-123"

        vedleggEtterspurtEvent(soknadsId, vedleggsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "vedleggEtterspurt").shouldBeNull()
    }
}
