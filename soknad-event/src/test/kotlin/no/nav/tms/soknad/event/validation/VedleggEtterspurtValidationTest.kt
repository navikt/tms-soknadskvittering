package no.nav.tms.soknad.event.validation

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tms.soknad.event.SoknadEvent
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggEtterspurtValidationTest {

    private val validEvent = vedleggEtterspurt()

    private val text10Chars = "Laaaaaaang"

    @Test
    fun `godkjenner gyldig vedleggEtterspurt-event`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            VedleggEtterspurtValidation.validate(validEvent)
        }
    }

    @Test
    fun `tillater at soknadsId er ugyldig`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                soknadsId = "badId"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis tittel er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                tittel = "${text10Chars.repeat(25)}++++++"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til ettersending er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkEttersending = "badLink"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkEttersending = "https://${text10Chars.repeat(20)}"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis vedleggsId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                vedleggsId = "${text10Chars.repeat(4)}+"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis etterspurt beskrivelse er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                beskrivelse = "${text10Chars.repeat(50)}+"
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis bruker er avsender og link mangler`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                brukerErAvsender = true,
                linkEttersending = null
            ).let {
                VedleggEtterspurtValidation.validate(it)
            }
        }
    }


    private fun vedleggEtterspurt() = SoknadEvent.VedleggEtterspurt(
        soknadsId = UUID.randomUUID().toString(),
        vedleggsId = "vedlegg-1",
        tittel = "Tittel på vedlegg",
        beskrivelse = null,
        brukerErAvsender = true,
        linkEttersending = "https://link.til.ettersending",
        tidspunktEtterspurt = ZonedDateTime.now(),
        produsent = SoknadEvent.Dto.Produsent("dev", "team", "app"),
        metadata = mapOf("meta" to "data"),
    )
}

