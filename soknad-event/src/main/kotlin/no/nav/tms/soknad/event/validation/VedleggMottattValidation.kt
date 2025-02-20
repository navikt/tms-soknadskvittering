package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent.VedleggMottatt


object VedleggMottattValidation {

    private val validators: List<VedleggMottattValidator> = listOf(
        VedleggsIdLengthValidator,
        TittelValidator,
        LinkVedleggValidator
    )

    fun validate(vedleggMottatt: VedleggMottatt) = validators.validate(vedleggMottatt)

    private interface VedleggMottattValidator: Validator<VedleggMottatt>

    private object TittelValidator: VedleggMottattValidator {
        private val validator = TextLengthValidator("Tittel for mottatt vedlegg", 255)

        override val description = validator.description

        override fun validate(event: VedleggMottatt) = assertTrue {
            validator.validate(event.tittel)
        }
    }

    private object VedleggsIdLengthValidator: VedleggMottattValidator {
        private val validator = TextLengthValidator("VedleggsId", 40)

        override val description = validator.description

        override fun validate(event: VedleggMottatt) = assertTrue {
            validator.validate(event.vedleggsId)
        }
    }

    private object LinkVedleggValidator: VedleggMottattValidator {
        override val description = "Feil i link til mottatt vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: VedleggMottatt) = assertTrue {
            LinkContentValidator.validate(event.linkVedlegg)
        }
    }
}
