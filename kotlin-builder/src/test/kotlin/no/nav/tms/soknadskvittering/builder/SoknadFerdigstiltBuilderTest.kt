package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.SoknadEvent.Dto.Produsent
import no.nav.tms.soknad.event.validation.SoknadsKvitteringValidationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SoknadFerdigstiltBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
    }

    @Test
    fun `lager ferdigstilt-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val soknadFerdigstilt = SoknadEventBuilder.ferdigstilt {
            soknadsId = testSoknadsId
            produsent = Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(soknadFerdigstilt).let { json ->
            json["@event_name"].asText() shouldBe "soknadFerdigstilt"
            json["soknadsId"].asText() shouldBe testSoknadsId

            json["produsent"].let {
                it["cluster"].asText() shouldBe "cluster"
                it["namespace"].asText() shouldBe "namespace"
                it["appnavn"].asText() shouldBe "app"
            }

            json["metadata"].let {
                it["version"].asText() shouldBe SoknadEvent.version
                it["built_at"].isNull shouldBe false
            }
        }
    }

    @Test
    fun `henter info om produsent automatisk der det er mulig`() {
        mapOf(
            "NAIS_APP_NAME" to "test-app",
            "NAIS_NAMESPACE" to "test-namespace",
            "NAIS_CLUSTER_NAME" to "dev"
        ).let { naisEnv ->
            BuilderEnvironment.extend(naisEnv)
        }

        val testSoknadsId = UUID.randomUUID().toString()

        val soknadFerdigstilt = SoknadEventBuilder.ferdigstilt {
            soknadsId = testSoknadsId
        }

        objectMapper.readTree(soknadFerdigstilt).let { json ->
            json["produsent"].let {
                it["cluster"].asText() shouldBe "dev"
                it["namespace"].asText() shouldBe "test-namespace"
                it["appnavn"].asText() shouldBe "test-app"
            }
        }
    }

    @Test
    fun `feiler hvis produsent ikke er satt og det ikke kan hentes automatisk`() {
        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.ferdigstilt {
                soknadsId = UUID.randomUUID().toString()
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.SoknadFerdigstiltInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            produsent = Produsent("cluster", "namespace", "app")
        }

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.ferdigstilt(validInstance) { soknadsId = null }
        }
    }
}
