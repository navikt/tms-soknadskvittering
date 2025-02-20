package no.nav.tms.soknad.event.validation

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tms.soknad.event.SoknadEvent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class SoknadOppdatertValidationTest{

    private val validEvent = soknadOppdatert()

    private val text10Chars = "Laaaaaaang"

    @Test
    fun `godkjenner gyldig oppdatert-event`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            SoknadOppdatertValidation.validate(validEvent)
        }
    }

    @Test
    fun `tillater at soknadsId er ugyldig`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                soknadsId = "badId"
            ).let {
                SoknadOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis journalpostId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                journalpostId = "${text10Chars.repeat(2)}+"
            ).let {
                SoknadOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til søknad er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkSoknad = "badLink"
            ).let {
                SoknadOppdatertValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkSoknad = "https://${text10Chars.repeat(20)}"
            ).let {
                SoknadOppdatertValidation.validate(it)
            }
        }
    }


    private fun soknadOppdatert() = SoknadEvent.SoknadOppdatert(
        soknadsId = UUID.randomUUID().toString(),
        fristEttersending = LocalDate.now().plusDays(21),
        linkSoknad = "https://annen.link.til.soknad",
        journalpostId = "456789",
        produsent = SoknadEvent.Dto.Produsent("dev", "team", "app"),
        metadata = mapOf("meta" to "data"),
    )
}
