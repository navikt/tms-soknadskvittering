package no.nav.tms.soknadskvittering.subscribers

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import no.nav.tms.soknadskvittering.common.shouldBeSameTimeAs
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import org.junit.jupiter.api.Test
import java.util.*

class VedleggMottattSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val messageBroadcaster = MessageBroadcaster(
        SoknadOpprettetSubscriber(repository),
        VedleggMottattSubscriber(repository)
    )

    @Test
    fun `legger til mottatte vedlegg for soknad`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let {
            messageBroadcaster.broadcastJson(it)
        }

        val vedleggsId = "vedlegg-1"
        val tittel = "Vedlegg om ett eller annet"
        val linkVedlegg = "https://link.til.vedlegg"
        val brukerErAvsender = true
        val tidspunktMottatt = ZonedDateTimeHelper.nowAtUtc()


        vedleggMottattEvent(
            soknadsId = soknadsId,
            vedleggsId = vedleggsId,
            tittel = tittel,
            linkVedlegg = linkVedlegg,
            brukerErAvsender = brukerErAvsender,
            tidspunktMottatt = tidspunktMottatt
        ).let { messageBroadcaster.broadcastJson(it) }

        val kvittering = repository.getSoknadsKvittering(soknadsId)!!

        kvittering.mottatteVedlegg.size shouldBe 1

        kvittering.mottatteVedlegg.first { it.vedleggsId == vedleggsId }.let {
            it.tittel shouldBe tittel
            it.linkVedlegg shouldBe linkVedlegg
            it.brukerErAvsender shouldBe brukerErAvsender
            it.tidspunktMottatt shouldBe tidspunktMottatt
        }
    }

    @Test
    fun `markerer etterspurt vedlegg som mottatt`() {
        val vedleggsId = "vedlegg-1"

        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(
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

        opprettetEvent(
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

}
