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
import no.nav.tms.soknad.event.validation.SoknadsKvitteringValidationException
import no.nav.tms.soknad.event.validation.VedleggEtterspurtValidation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggEtterspurtTredjepartBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(VedleggEtterspurtValidation)
    }

    @Test
    fun `lager vedleggEtterspurt-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val vedleggEtterspurtTredjepart = SoknadEventBuilder.vedleggEtterspurtTredjepart {
            soknadsId = testSoknadsId
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            beskrivelse = "Mer detaljert beskrivelse av vedlegg"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(vedleggEtterspurtTredjepart).let { json ->
            json["@event_name"].asText() shouldBe "vedleggEtterspurt"
            json["brukerErAvsender"].asBoolean() shouldBe false
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["tittel"].asText() shouldBe "Vedlegg som etterspørres"
            json["vedleggsId"].asText() shouldBe "vedlegg-1"
            json["beskrivelse"].asText() shouldBe "Mer detaljert beskrivelse av vedlegg"
            json["tidspunktEtterspurt"].asText() shouldBe "2025-02-01T10:00:00Z"

            json["linkEttersending"].shouldBeNull()

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

        val vedleggEtterspurtTredjepart = SoknadEventBuilder.vedleggEtterspurtTredjepart {
            soknadsId = testSoknadsId
            tittel = "Vedlegg som etterspørres"
            vedleggsId = "vedlegg-1"
            beskrivelse = "Mer detaljert beskrivelse av vedlegg"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
        }

        objectMapper.readTree(vedleggEtterspurtTredjepart).let { json ->
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
            SoknadEventBuilder.vedleggEtterspurtTredjepart {
                soknadsId = UUID.randomUUID().toString()
                tittel = "Vedlegg som etterspørres"
                vedleggsId = "vedlegg-1"
                beskrivelse = "Mer detaljert beskrivelse av vedlegg"
                tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.VedleggEtterspurtTredjepartInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som etterspørres"
            beskrivelse = "Mer detaljert beskrivelse av vedlegg"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) { beskrivelse = null }
        }

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) { soknadsId = null }
        }

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) { vedleggsId = null }
        }

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) { tittel = null }
        }

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) { tidspunktEtterspurt = null }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.VedleggEtterspurtTredjepartInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som etterspørres"
            beskrivelse = "Mer detaljert beskrivelse av vedlegg"
            tidspunktEtterspurt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        mockkObject(VedleggEtterspurtValidation)

        every { VedleggEtterspurtValidation.validate(any()) } throws SoknadsKvitteringValidationException("")

        shouldThrow<SoknadsKvitteringValidationException> {
            SoknadEventBuilder.vedleggEtterspurtTredjepart(validInstance) {}
        }
    }
}
