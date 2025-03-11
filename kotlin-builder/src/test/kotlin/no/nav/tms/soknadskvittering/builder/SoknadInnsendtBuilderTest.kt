package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.SoknadEvent.Dto.Produsent
import no.nav.tms.soknad.event.validation.SoknadInnsendtValidation
import no.nav.tms.soknad.event.validation.SoknadskvitteringValidationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class SoknadInnsendtBuilderTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun cleanUp() {
        BuilderEnvironment.reset()
        unmockkObject(SoknadInnsendtValidation)
    }

    @Test
    fun `lager opprett-event på ventet format`() {

        val testSoknadsId = UUID.randomUUID().toString()

        val soknadInnsendt = SoknadEventBuilder.innsendt {
            soknadsId = testSoknadsId
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            linkEttersending = "https://link.til.ettersending"
            journalpostId = "123456"
            produsent = Produsent("cluster", "namespace", "app")

            mottattVedlegg {
                vedleggsId = "vedlegg-1"
                tittel = "Vedlegg om noe"
                linkVedlegg = "https://link.til.vedlegg"
                journalpostId = "en-journalpostId"
            }

            mottattVedlegg {
                vedleggsId = "vedlegg-2"
                tittel = "Vedlegg om noe annet"
                linkVedlegg = "https://link.til.annet.vedlegg"
            }

            etterspurtVedlegg {
                vedleggsId = "vedlegg-3"
                tittel = "Vedlegg som mangler"
                brukerErAvsender = true
                linkEttersending = "https://link.til.ettersending"
            }
        }

        objectMapper.readTree(soknadInnsendt).let { json ->
            json["@event_name"].asText() shouldBe "soknadInnsendt"
            json["soknadsId"].asText() shouldBe testSoknadsId
            json["ident"].asText() shouldBe "12345678910"
            json["tittel"].asText() shouldBe "Soknadstittel"
            json["temakode"].asText() shouldBe "TEM"
            json["skjemanummer"].asText() shouldBe "Skjema-123"
            json["tidspunktMottatt"].asText() shouldBe "2025-02-01T10:00:00Z"
            json["fristEttersending"].asText() shouldBe "2025-03-01"
            json["linkSoknad"].asText() shouldBe "https://link.til.soknad"
            json["linkEttersending"].asText() shouldBe "https://link.til.ettersending"
            json["journalpostId"].asText() shouldBe "123456"

            json["mottatteVedlegg"].size() shouldBe 2
            json["mottatteVedlegg"][0].let { vedlegg ->
                vedlegg["vedleggsId"].asText() shouldBe "vedlegg-1"
                vedlegg["tittel"].asText() shouldBe "Vedlegg om noe"
                vedlegg["linkVedlegg"].asText() shouldBe "https://link.til.vedlegg"
                vedlegg["journalpostId"].asText() shouldBe "en-journalpostId"
            }
            json["mottatteVedlegg"][1].let { vedlegg ->
                vedlegg["vedleggsId"].asText() shouldBe "vedlegg-2"
                vedlegg["tittel"].asText() shouldBe "Vedlegg om noe annet"
                vedlegg["linkVedlegg"].asText() shouldBe "https://link.til.annet.vedlegg"
            }

            json["etterspurteVedlegg"].size() shouldBe 1
            json["etterspurteVedlegg"][0].let { vedlegg ->
                vedlegg["vedleggsId"].asText() shouldBe "vedlegg-3"
                vedlegg["tittel"].asText() shouldBe "Vedlegg som mangler"
                vedlegg["brukerErAvsender"].asBoolean() shouldBe true
                vedlegg["linkEttersending"].asText() shouldBe "https://link.til.ettersending"
            }

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

        val soknadInnsendt = SoknadEventBuilder.innsendt {
            soknadsId = UUID.randomUUID().toString()
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            journalpostId = "123456"
        }

        objectMapper.readTree(soknadInnsendt).let { json ->
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
            SoknadEventBuilder.innsendt {
                soknadsId = UUID.randomUUID().toString()
                ident = "12345678910"
                tittel = "Soknadstittel"
                temakode = "TEM"
                skjemanummer = "Skjema-123"
                tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
                fristEttersending = LocalDate.parse("2025-03-01")
                linkSoknad = "https://link.til.soknad"
                journalpostId = "123456"
            }
        }
    }

    @Test
    fun `feiler hvis påkrevde felt er null`() {

        val validInstance = SoknadEventBuilder.SoknadInnsendtInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            linkEttersending = "https://link.til.ettersending"
            journalpostId = "123456"
            produsent = Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.innsendt(validInstance) {
                linkSoknad = null
                linkEttersending = null
                journalpostId = null
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { soknadsId = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { ident = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { tittel = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { temakode = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { skjemanummer = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { tidspunktMottatt = null }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) { fristEttersending = null }
        }
    }

    @Test
    fun `feiler hvis påkrevede felt for mottatte vedlegg er null`() {
        val validInstance = SoknadEventBuilder.SoknadInnsendtInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            journalpostId = "123456"
            produsent = Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.innsendt(validInstance) {
                mottattVedlegg {
                    vedleggsId = "dummy"
                    tittel = "dummy"
                    linkVedlegg = null
                    journalpostId = null
                }
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {
                mottattVedlegg {
                    vedleggsId = "dummy"
                    tittel = null
                    linkVedlegg = "https://dummy"
                    journalpostId = "dummy"
                }
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {
                mottattVedlegg {
                    vedleggsId = null
                    tittel = "dummy"
                    linkVedlegg = "https://dummy"
                    journalpostId = "dummy"
                }
            }
        }
    }

    @Test
    fun `feiler hvis påkrevede felt for etterspurte vedlegg er null`() {
        val validInstance = SoknadEventBuilder.SoknadInnsendtInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            journalpostId = "123456"
            produsent = Produsent("cluster", "namespace", "app")
        }

        shouldNotThrowAny {
            SoknadEventBuilder.innsendt(validInstance) {
                etterspurtVedlegg {
                    vedleggsId = "dummy"
                    tittel = "dummy"
                    brukerErAvsender = true
                    linkEttersending = null
                }
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {
                etterspurtVedlegg {
                    vedleggsId = null
                    tittel = "dummy"
                    brukerErAvsender = true
                    linkEttersending = "https://dummy"
                }
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {
                etterspurtVedlegg {
                    vedleggsId = "dummy"
                    tittel = null
                    brukerErAvsender = true
                    linkEttersending = "https://dummy"
                }
            }
        }

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {
                etterspurtVedlegg {
                    vedleggsId = "dummy"
                    tittel = "dummy"
                    brukerErAvsender = null
                    linkEttersending = "https://dummy"
                }
            }
        }
    }

    @Test
    fun `feiler hvis eventet ikke er gyldig`() {

        val validInstance = SoknadEventBuilder.SoknadInnsendtInstance().apply {
            soknadsId = UUID.randomUUID().toString()
            ident = "12345678910"
            tittel = "Soknadstittel"
            temakode = "TEM"
            skjemanummer = "Skjema-123"
            tidspunktMottatt = ZonedDateTime.parse("2025-02-01T10:00:00Z")
            fristEttersending = LocalDate.parse("2025-03-01")
            linkSoknad = "https://link.til.soknad"
            journalpostId = "123456"
            produsent = Produsent("cluster", "namespace", "app")
        }

        mockkObject(SoknadInnsendtValidation)

        every { SoknadInnsendtValidation.validate(any()) } throws SoknadskvitteringValidationException("")

        shouldThrow<SoknadskvitteringValidationException> {
            SoknadEventBuilder.innsendt(validInstance) {}
        }
    }
}
