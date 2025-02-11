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
import no.nav.tms.soknad.event.validation.VedleggMottattValidation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggMottattBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(VedleggMottattValidation)
    }

    @Test
    fun `lager vedleggMottatt-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val vedleggMottatt = SoknadEventBuilder.vedleggMottatt {
            soknadsId = testSoknadsId
            brukerErAvsender = true
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som er mottatt"
            linkVedlegg = "https://link.til.vedlegg"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        objectMapper.readTree(vedleggMottatt).let { json ->
            json["@event_name"].asText() shouldBe "vedleggMottatt"
            json["brukerErAvsender"].asBoolean() shouldBe true
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["vedleggsId"].asText() shouldBe "vedlegg-1"
            json["tittel"].asText() shouldBe "Vedlegg som er mottatt"
            json["linkVedlegg"].asText() shouldBe "https://link.til.vedlegg"
            json["tidspunktMottatt"].asText() shouldBe "2025-02-01T10:00:00Z"

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

        val vedleggMottatt = SoknadEventBuilder.vedleggMottatt {
            soknadsId = testSoknadsId
            brukerErAvsender = true
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som er mottatt"
            linkVedlegg = "https://link.til.vedlegg"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
        }

        objectMapper.readTree(vedleggMottatt).let { json ->
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
            SoknadEventBuilder.vedleggMottatt {
                soknadsId = UUID.randomUUID().toString()
                brukerErAvsender = true
                vedleggsId = "vedlegg-1"
                tittel = "Vedlegg som er mottatt"
                linkVedlegg = "https://link.til.vedlegg"
                tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.VedleggMottattInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            brukerErAvsender = true
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som er mottatt"
            linkVedlegg = "https://link.til.vedlegg"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.vedleggMottatt(validInstance) { linkVedlegg = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) { soknadsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) { brukerErAvsender = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) { vedleggsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) { tittel = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) { tidspunktMottatt = null }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.VedleggMottattInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            brukerErAvsender = true
            vedleggsId = "vedlegg-1"
            tittel = "Vedlegg som er mottatt"
            linkVedlegg = "https://link.til.vedlegg"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            produsent = SoknadEvent.Dto.Produsent("cluster", "namespace", "app")
        }

        mockkObject(VedleggMottattValidation)

        every { VedleggMottattValidation.validate(any()) } throws SoknadskvitteringValidationException("")

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.vedleggMottatt(validInstance) {}
        }
    }
}
