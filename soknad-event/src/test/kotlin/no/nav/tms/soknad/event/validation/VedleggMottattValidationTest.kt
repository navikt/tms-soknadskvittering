package no.nav.tms.soknad.event.validation

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tms.soknad.event.SoknadEvent
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class VedleggMottattValidationTest {

    private val validEvent = vedleggMottatt()

    private val text10Chars = "Laaaaaaang"

    @Test
    fun `godkjenner gyldig vedleggMottatt-event`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            VedleggMottattValidation.validate(validEvent)
        }
    }

    @Test
    fun `tillater at soknadsId er ugyldig`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                soknadsId = "badId"
            ).let {
                VedleggMottattValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis tittel er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                tittel = "${text10Chars.repeat(25)}++++++"
            ).let {
                VedleggMottattValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til vedlegg er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkVedlegg = "badLink"
            ).let {
                VedleggMottattValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkVedlegg = "https://${text10Chars.repeat(20)}"
            ).let {
                VedleggMottattValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis vedleggsId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                vedleggsId = "${text10Chars.repeat(4)}+"
            ).let {
                VedleggMottattValidation.validate(it)
            }
        }
    }

    private fun vedleggMottatt() = SoknadEvent.VedleggMottatt(
        soknadsId = UUID.randomUUID().toString(),
        vedleggsId = "vedlegg-1",
        tittel = "Tittel på vedlegg",
        brukerErAvsender = true,
        linkVedlegg = "https://link.til.vedlegg",
        tidspunktMottatt = ZonedDateTime.now(),
        produsent = SoknadEvent.Dto.Produsent("dev", "team", "app"),
        metadata = mapOf("meta" to "data"),
    )
}

