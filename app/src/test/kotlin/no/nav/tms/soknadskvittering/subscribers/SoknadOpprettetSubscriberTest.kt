package no.nav.tms.soknadskvittering.subscribers

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.soknadskvittering.setup.LocalPostgresDatabase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SoknadOpprettetSubscriberTest {
    private val database = LocalPostgresDatabase.cleanDb()

    private val repository = SoknadsKvitteringRepository(database)

    private val messageBroadcaster = MessageBroadcaster(SoknadOpprettetSubscriber(repository))

    @Test
    fun `oppretter søknadskvittering i databasen`() {
        val soknadsId = UUID.randomUUID().toString()
        val ident = "12345678900"

        val event = opprettEvent(soknadsId, ident)

        messageBroadcaster.broadcastJson(event)
        messageBroadcaster.broadcastJson(event)

        repository.getSoknadsKvittering(soknadsId).shouldNotBeNull()
        repository.getSoknadskvitteringForUser(ident).size shouldBe 1
    }
}
