package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent.VedleggEtterspurt


object VedleggEtterspurtValidation {

    private val validators: List<VedleggEtterspurtValidator> = listOf(
        VedleggsIdLengthValidator,
        TittelValidator,
        LinkEttersendingValidator,
        BeskrivelseValidator,
        BrukerErAvsenderValidator
    )

    fun validate(vedleggEtterspurt: VedleggEtterspurt) = validators.validate(vedleggEtterspurt)

    private interface VedleggEtterspurtValidator: Validator<VedleggEtterspurt>

    private object TittelValidator: VedleggEtterspurtValidator {
        private val validator = TextLengthValidator("Tittel for etterspurte vedlegg", 255)

        override val description = validator.description

        override fun validate(event: VedleggEtterspurt) = assertTrue {
            validator.validate(event.tittel)
        }
    }

    private object BeskrivelseValidator: VedleggEtterspurtValidator {
        private val validator = TextLengthValidator("Beskrivelse for etterspurte vedlegg", 500)

        override val description = validator.description

        override fun validate(event: VedleggEtterspurt) = assertTrue {
            validator.validate(event.beskrivelse)
        }
    }

    private object VedleggsIdLengthValidator: VedleggEtterspurtValidator {
        private val validator = TextLengthValidator("VedleggsId", 40)

        override val description = validator.description

        override fun validate(event: VedleggEtterspurt) = assertTrue {
            validator.validate(event.vedleggsId)
        }
    }

    private object LinkEttersendingValidator: VedleggEtterspurtValidator {
        override val description = "Feil i link for å ettersende vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: VedleggEtterspurt) = assertTrue {
            LinkContentValidator.validate(event.linkEttersending)
        }
    }

    private object BrukerErAvsenderValidator: VedleggEtterspurtValidator {
        override val description = "Link til ettersending er påkrevd dersom bruker skal sende inn"

        override fun validate(event: VedleggEtterspurt) = assertTrue {
            if (event.brukerErAvsender) {
                event.linkEttersending != null
            } else {
                true
            }
        }
    }
}
