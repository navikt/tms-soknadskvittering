package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.SoknadEvent.Dto.Produsent
import no.nav.tms.soknad.event.validation.SoknadOppdatertValidation
import no.nav.tms.soknad.event.validation.SoknadskvitteringValidationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SoknadOppdatertBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(SoknadOppdatertValidation)
    }

    @Test
    fun `lager oppdatert-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val soknadOppdatert = SoknadEventBuilder.oppdatert {
            soknadsId = testSoknadsId
            fristEttersending = LocalDate.parse("2025-04-01")
            linkSoknad = "https://link.til.soknad.oppdatert"
            journalpostId = "1234567"
            produsent = Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(soknadOppdatert).let { json ->
            json["@event_name"].asText() shouldBe "soknadOppdatert"
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["fristEttersending"].asText() shouldBe "2025-04-01"
            json["linkSoknad"].asText() shouldBe "https://link.til.soknad.oppdatert"
            json["journalpostId"].asText() shouldBe "1234567"

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
    fun `lager event på ventet format når felt ikke er spesifisert`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val soknadOppdatert = SoknadEventBuilder.oppdatert {
            soknadsId = testSoknadsId
            produsent = Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(soknadOppdatert).let { json ->
            json["@event_name"].asText() shouldBe "soknadOppdatert"
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["fristEttersending"].shouldBeNull()
            json["linkSoknad"].shouldBeNull()
            json["journalpostId"].shouldBeNull()

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

        val soknadOppdatert = SoknadEventBuilder.oppdatert {
            soknadsId = testSoknadsId
        }

        objectMapper.readTree(soknadOppdatert).let { json ->
            json["produsent"].let {
                it["cluster"].asText() shouldBe "dev"
                it["namespace"].asText() shouldBe "test-namespace"
                it["appnavn"].asText() shouldBe "test-app"
            }
        }
    }

    @Test
    fun `feiler hvis produsent ikke er satt og det ikke kan hentes automatisk`() {
        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.oppdatert {
                soknadsId = UUID.randomUUID().toString()
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.SoknadOppdatertInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            fristEttersending = LocalDate.parse("2025-04-01")
            linkSoknad = "https://link.til.soknad.oppdatert"
            journalpostId = "1234567"
            produsent = Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.oppdatert(validInstance) {
                linkSoknad = null
                journalpostId = null
                fristEttersending = null
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.oppdatert(validInstance) { soknadsId = null }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.SoknadOppdatertInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            fristEttersending = LocalDate.parse("2025-04-01")
            linkSoknad = "https://link.til.soknad.oppdatert"
            journalpostId = "1234567"
            produsent = Produsent("cluster", "namespace", "app")
        }

        mockkObject(SoknadOppdatertValidation)

        every { SoknadOppdatertValidation.validate(any()) } throws SoknadskvitteringValidationException("")

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.oppdatert(validInstance) {}
        }
    }
}
