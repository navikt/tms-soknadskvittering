package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent.SoknadInnsendt


object SoknadInnsendtValidation {

    private const val BASE_16 = "[0-9a-fA-F]"
    private const val BASE_32_ULID = "[0-9ABCDEFGHJKMNPQRSTVWXYZabcdefghjkmnpqrstvwxyz]"

    private val UUID_PATTERN = "^$BASE_16{8}-$BASE_16{4}-$BASE_16{4}-$BASE_16{4}-$BASE_16{12}$".toRegex()
    private val ULID_PATTERN = "^[0-7]$BASE_32_ULID{25}$".toRegex()

    private val validators: List<SoknadInnsendtValidator> = listOf(
        IdentValidator,
        SoknadsIdValidator,
        TittelLengthValidator,
        TemakodeLengthValidator,
        SkjemanummerLengthValidator,
        JournalpostIdLengthValidator,
        LinkSoknadValidator,
        LinkEttersendingValidator,

        VedleggsIdLengthValidator,
        VedleggsIdDuplicateValidator,
        VedleggAlleredeMottattValidator,
        MottatteVedleggTittelLengthValidator,
        MottatteVedleggLinkContentValidator,
        MottatteVedleggJournalpostIdValidator,
        EtterspurteVedleggTittelLengthValidator,
        EtterspurteVedleggLinkContentValidator,
        EtterspurteVedleggBeskrivelseValidator
    )

    fun validate(soknadInnsendt: SoknadInnsendt) = validators.validate(soknadInnsendt)

    private interface SoknadInnsendtValidator: Validator<SoknadInnsendt>

    private object IdentValidator: SoknadInnsendtValidator {
        override val description: String = "Fodselsnummer må være 11 tegn"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.ident.length == 11
        }
    }

    private object SoknadsIdValidator: SoknadInnsendtValidator {
        override val description: String = "Eventid må være gyldig UUID eller ULID"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.soknadsId.let {
                UUID_PATTERN.matches(it) || ULID_PATTERN.matches(it)
            }
        }
    }

    private object TemakodeLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Temakode", 3)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            validator.validate(event.temakode)
        }
    }

    private object TittelLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Soknadstittel", 80)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            validator.validate(event.tittel)
        }
    }

    private object SkjemanummerLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Skjemmanummer", 40)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            validator.validate(event.skjemanummer)
        }
    }

    private object JournalpostIdLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("JournalpostId", 20)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            validator.validate(event.journalpostId)
        }
    }

    private object LinkSoknadValidator: SoknadInnsendtValidator {
        override val description = LinkContentValidator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            LinkContentValidator.validate(event.linkSoknad)
        }
    }

    private object LinkEttersendingValidator: SoknadInnsendtValidator {
        override val description = LinkContentValidator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            LinkContentValidator.validate(event.linkEttersending)
        }
    }

    private object MottatteVedleggTittelLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Tittel for mottatte vedlegg", 255)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.all {
                validator.validate(it.tittel)
            }
        }
    }

    private object EtterspurteVedleggTittelLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Tittel for etterspurte vedlegg", 255)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.etterspurteVedlegg.all {
                validator.validate(it.tittel)
            }
        }
    }

    private object EtterspurteVedleggBeskrivelseValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("Beskrivelse for etterspurte vedlegg", 500)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.etterspurteVedlegg.all {
                validator.validate(it.beskrivelse)
            }
        }
    }


    private object VedleggsIdLengthValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("VedleggsId", 40)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.all { validator.validate(it.vedleggsId) } &&
                event.etterspurteVedlegg.all { validator.validate(it.vedleggsId) }
        }
    }

    private object VedleggAlleredeMottattValidator: SoknadInnsendtValidator {
        override val description = "Kan ikke si at et etterspurt vedlegg er mottatt i samme event"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.map { it.vedleggsId }.none { vedleggsId ->
                event.etterspurteVedlegg.map { it.vedleggsId }.contains(vedleggsId)
            }
        }
    }

    private object VedleggsIdDuplicateValidator: SoknadInnsendtValidator {
        override val description = "Kan ikke gjenbruke samme vedleggsId flere ganger for én søknad"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.map { it.vedleggsId }.let { it.size == it.distinct().size } &&
                event.etterspurteVedlegg.map { it.vedleggsId }.let { it.size == it.distinct().size }
        }
    }

    private object MottatteVedleggLinkContentValidator: SoknadInnsendtValidator {
        override val description = "Feil i link til mottatt vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.all {
                LinkContentValidator.validate(it.linkVedlegg)
            }
        }
    }

    private object MottatteVedleggJournalpostIdValidator: SoknadInnsendtValidator {
        private val validator = TextLengthValidator("journalpostId for mottatt vedlegg", 20)

        override val description = validator.description

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.mottatteVedlegg.all {
                validator.validate(it.journalpostId)
            }
        }
    }

    private object EtterspurteVedleggLinkContentValidator: SoknadInnsendtValidator {
        override val description = "Feil i link for å ettersende vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: SoknadInnsendt) = assertTrue {
            event.etterspurteVedlegg.all {
                LinkContentValidator.validate(it.linkEttersending)
            }
        }
    }
}
