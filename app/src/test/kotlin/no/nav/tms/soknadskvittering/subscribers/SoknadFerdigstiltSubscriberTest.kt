package no.nav.tms.soknadskvittering.subscribers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.common.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*

class SoknadFerdigstiltSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = SoknadsKvitteringRepository(database)

    private val messageBroadcaster = MessageBroadcaster(
        SoknadOpprettetSubscriber(repository),
        SoknadFerdigstiltSubscriber(repository)
    )

    @AfterEach
    fun cleanUp() {
        database.update { queryOf("delete from soknadskvittering") }
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
}
