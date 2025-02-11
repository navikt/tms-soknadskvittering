package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.validation.SoknadskvitteringValidationException
import no.nav.tms.soknad.event.validation.VedleggEtterspurtValidation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggEtterspurtBrukerBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(VedleggEtterspurtValidation)
    }

    @Test
    fun `lager vedleggEtterspurt-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val vedleggEtterspurtBruker = SoknadEventBuilder.vedleggEtterspurtBruker {
            soknadsId = testSoknadsId
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            linkEttersending = "https://link.til.ettersending"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(vedleggEtterspurtBruker).let { json ->
            json["@event_name"].asText() shouldBe "vedleggEtterspurt"
            json["brukerErAvsender"].asBoolean() shouldBe true
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["tittel"].asText() shouldBe "Vedlegg som etterspørres"
            json["vedleggsId"].asText() shouldBe "vedlegg-1"
            json["linkEttersending"].asText() shouldBe "https://link.til.ettersending"
            json["tidspunktEtterspurt"].asText() shouldBe "2025-02-01T10:00:00Z"

            json["beskrivelse"].shouldBeNull()

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

        val vedleggEtterspurtBruker = SoknadEventBuilder.vedleggEtterspurtBruker {
            soknadsId = testSoknadsId
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            linkEttersending = "https://link.til.ettersending"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
        }

        objectMapper.readTree(vedleggEtterspurtBruker).let { json ->
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
            SoknadEventBuilder.vedleggEtterspurtBruker {
                soknadsId = UUID.randomUUID().toString()
                tittel = "Vedlegg som etterspørres"
                vedleggsId = "vedlegg-1"
                linkEttersending = "https://link.til.ettersending"
                tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.VedleggEtterspurtBrukerInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            linkEttersending = "https://link.til.ettersending"
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) { soknadsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) { vedleggsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) { tittel = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) { linkEttersending = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) { tidspunktEtterspurt = null }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.VedleggEtterspurtBrukerInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            linkEttersending = "https://link.til.ettersending"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        mockkObject(VedleggEtterspurtValidation)

        every { VedleggEtterspurtValidation.validate(any()) } throws SoknadskvitteringValidationException("")

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtBruker(validInstance) {}
        }
    }
}
