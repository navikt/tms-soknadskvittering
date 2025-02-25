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
import java.util.*

class SoknadFerdigstiltSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val appender = HistorikkAppender(HistorikkRepository(database))

    private val messageBroadcaster = MessageBroadcaster(
        SoknadOpprettetSubscriber(repository, appender),
        SoknadFerdigstiltSubscriber(repository, appender)
    )

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadskvittering") }
        database.update { queryOf("delete from soknadsevent_historikk") }
    }

    @Test
    fun `markerer soknad som ferdigstilt`() {
        val soknadsId1 = UUID.randomUUID().toString()
        val soknadsId2 = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId1, ident).let { messageBroadcaster.broadcastJson(it) }
        opprettetEvent(soknadsId2, ident).let { messageBroadcaster.broadcastJson(it) }

        ferdigstiltEvent(soknadsId1).let { messageBroadcaster.broadcastJson(it) }

        repository.getSoknadsKvittering(soknadsId1)!!.ferdigstilt.shouldNotBeNull()
        repository.getSoknadsKvittering(soknadsId2)!!.ferdigstilt.shouldBeNull()
    }

    @Test
    fun `ignorerer oppdatering for ukjente soknadsIder`() {
        val soknadsId = UUID.randomUUID().toString()

        ferdigstiltEvent(soknadsId).let {
            messageBroadcaster.broadcastJson(it)
        }

        database.single {
            queryOf("select count(*) as antall from soknadskvittering")
                .map { it.int("antall") }.asSingle
        } shouldBe 0
    }

    @Test
    fun `legger til hendelse i event-historikk når søknad blir ferdigstilt`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        opprettetEvent(soknadsId, ident).let { messageBroadcaster.broadcastJson(it) }
        ferdigstiltEvent(soknadsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "soknadFerdigstilt").shouldNotBeNull()
    }

    @Test
    fun `legger ikke til hendelse i event-historikk hvis søknad ikke fantes`() {
        val soknadsId = UUID.randomUUID().toString()

        ferdigstiltEvent(soknadsId).let { messageBroadcaster.broadcastJson(it) }

        database.firstHistorikkEntry(soknadsId, "soknadFerdigstilt").shouldBeNull()
    }
}
