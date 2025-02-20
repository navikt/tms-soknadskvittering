package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent

object SoknadOppdatertValidation {

    private val validators: List<SoknadOppdatertValidator> = listOf(
        JournalpostIdLengthValidator,
        LinkSoknadValidator
    )

    fun validate(soknadOppdatert: SoknadEvent.SoknadOppdatert) = validators.validate(soknadOppdatert)

    private interface SoknadOppdatertValidator: Validator<SoknadEvent.SoknadOppdatert>

    private object JournalpostIdLengthValidator: SoknadOppdatertValidator {
        private val validator = TextLengthValidator("JournalpostId", 20)

        override val description = validator.description

        override fun validate(event: SoknadEvent.SoknadOppdatert) = assertTrue {
            validator.validate(event.journalpostId)
        }
    }

    private object LinkSoknadValidator: SoknadOppdatertValidator {
        override val description = LinkContentValidator.description

        override fun validate(event: SoknadEvent.SoknadOppdatert) = assertTrue {
            LinkContentValidator.validate(event.linkSoknad)
        }
    }

}
