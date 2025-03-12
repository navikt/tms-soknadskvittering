package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.SoknadEvent.VedleggOppdatert


object VedleggOppdatertValidation {

    private val validators: List<VedleggOppdatertValidator> = listOf(
        LinkVedleggValidator,
        JournalpostIdLengthValidator
    )

    fun validate(vedleggOppdatert: VedleggOppdatert) = validators.validate(vedleggOppdatert)

    private interface VedleggOppdatertValidator: Validator<VedleggOppdatert>

    private object LinkVedleggValidator: VedleggOppdatertValidator {
        override val description = "Feil i link til mottatt vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: VedleggOppdatert) = assertTrue {
            LinkContentValidator.validate(event.linkVedlegg)
        }
    }

    private object JournalpostIdLengthValidator: VedleggOppdatertValidator {
        private val validator = TextLengthValidator("JournalpostId", 20)

        override val description = validator.description

        override fun validate(event: SoknadEvent.VedleggOppdatert) = assertTrue {
            validator.validate(event.journalpostId)
        }
    }
}
