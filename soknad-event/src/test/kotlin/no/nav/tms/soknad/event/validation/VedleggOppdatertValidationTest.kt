package no.nav.tms.soknad.event.validation

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tms.soknad.event.SoknadEvent
import org.junit.jupiter.api.Test
import java.util.*

class VedleggOppdatertValidationTest {

    private val validEvent = vedleggOppdatert()

    private val text10Chars = "Laaaaaaang"

    @Test
    fun `godkjenner gyldig vedleggOppdatert-event`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            VedleggOppdatertValidation.validate(validEvent)
        }
    }

    @Test
    fun `tillater at soknadsId er ugyldig`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                soknadsId = "badId"
            ).let {
                VedleggOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til vedlegg er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkVedlegg = "badLink"
            ).let {
                VedleggOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til vedlegg er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkVedlegg = "https://${text10Chars.repeat(20)}"
            ).let {
                VedleggOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `tillater at vedleggsId er for lang`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                vedleggsId = "${text10Chars.repeat(4)}+"
            ).let {
                VedleggOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `tillater at journalpostId er for lang`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                vedleggsId = "${text10Chars.repeat(2)}+"
            ).let {
                VedleggOppdatertValidation.validate(it)
            }
        }
    }


    private fun vedleggOppdatert() = SoknadEvent.VedleggOppdatert(
        soknadsId = UUID.randomUUID().toString(),
        vedleggsId = "vedlegg-1",
        linkVedlegg = "https://ny.link.til.vedlegg",
        journalpostId = "123456",
        produsent = SoknadEvent.Dto.Produsent("dev", "team", "app"),
        metadata = mapOf("meta" to "data"),
    )
}


