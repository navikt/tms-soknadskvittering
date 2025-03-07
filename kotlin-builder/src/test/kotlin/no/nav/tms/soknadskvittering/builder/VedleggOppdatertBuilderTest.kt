package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.validation.SoknadskvitteringValidationException
import no.nav.tms.soknad.event.validation.VedleggOppdatertValidation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggOppdatertBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(VedleggOppdatertValidation)
    }

    @Test
    fun `lager vedleggOppdatert-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val vedleggOppdatert = SoknadEventBuilder.vedleggOppdatert {
            soknadsId = testSoknadsId
            vedleggsId = "vedlegg-1"
            linkVedlegg = "https://ny.link.til.vedlegg"
            journalpostId = "456123"
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(vedleggOppdatert).let { json ->
            json["@event_name"].asText() shouldBe "vedleggOppdatert"
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["vedleggsId"].asText() shouldBe "vedlegg-1"
            json["linkVedlegg"].asText() shouldBe "https://ny.link.til.vedlegg"
            json["journalpostId"].asText() shouldBe "456123"

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

        val vedleggOppdatert = SoknadEventBuilder.vedleggOppdatert {
            soknadsId = testSoknadsId
            vedleggsId = "vedlegg-1"
            linkVedlegg = "https://link.til.vedlegg"
        }

        objectMapper.readTree(vedleggOppdatert).let { json ->
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
            SoknadEventBuilder.vedleggOppdatert {
                soknadsId = UUID.randomUUID().toString()
                vedleggsId = "vedlegg-1"
                linkVedlegg = "https://link.til.vedlegg"
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.VedleggOppdatertInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            vedleggsId = "vedlegg-1"
            linkVedlegg = "https://link.til.vedlegg"
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.vedleggOppdatert(validInstance) {
                linkVedlegg = null
                journalpostId = null
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggOppdatert(validInstance) { soknadsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggOppdatert(validInstance) { vedleggsId = null }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.VedleggOppdatertInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            vedleggsId = "vedlegg-1"
            linkVedlegg = "https://link.til.vedlegg"
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        mockkObject(VedleggOppdatertValidation)

        every { VedleggOppdatertValidation.validate(any()) } throws SoknadskvitteringValidationException("")

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggOppdatert(validInstance) {}
        }
    }
}
