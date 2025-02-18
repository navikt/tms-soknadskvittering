package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent.SoknadOpprettet


object SoknadOpprettetValidation {

    private const val BASE_16 = "[0-9a-fA-F]"
    private const val BASE_32_ULID = "[0-9ABCDEFGHJKMNPQRSTVWXYZabcdefghjkmnpqrstvwxyz]"

    private val UUID_PATTERN = "^$BASE_16{8}-$BASE_16{4}-$BASE_16{4}-$BASE_16{4}-$BASE_16{12}$".toRegex()
    private val ULID_PATTERN = "^[0-7]$BASE_32_ULID{25}$".toRegex()

    private val validators: List<SoknadOpprettetValidator> = listOf(
        IdentValidator,
        SoknadsIdValidator,
        TittelLengthValidator,
        TemakodeLengthValidator,
        SkjemanummerLengthValidator,
        JournalpostIdLengthValidator,
        LinkSoknadValidator,

        VedleggsIdLengthValidator,
        VedleggsIdDuplicateValidator,
        VedleggAlleredeMottattValidator,
        MottatteVedleggTittelValidator,
        MottatteVedleggLinkContentValidator,
        EtterspurteVedleggTittelValidator,
        EtterspurteVedleggLinkContentValidator,
        EtterspurteVedleggBeskrivelseValidator,
        BrukerErAvsenderValidator
    )

    fun validate(opprettVarsel: SoknadOpprettet) = validators.validate(opprettVarsel)

    private interface SoknadOpprettetValidator: Validator<SoknadOpprettet>

    private object IdentValidator: SoknadOpprettetValidator {
        override val description: String = "Fodselsnummer må være 11 tegn"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.ident.length == 11
        }
    }

    private object SoknadsIdValidator: SoknadOpprettetValidator {
        override val description: String = "Eventid må være gyldig UUID eller ULID"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.soknadsId.let {
                UUID_PATTERN.matches(it) || ULID_PATTERN.matches(it)
            }
        }
    }

    private object TemakodeLengthValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Temakode", 3)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            validator.validate(event.temakode)
        }
    }

    private object TittelLengthValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Soknadstittel", 80)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            validator.validate(event.tittel)
        }
    }

    private object SkjemanummerLengthValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Skjemmanummer", 40)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            validator.validate(event.skjemanummer)
        }
    }

    private object JournalpostIdLengthValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("JournalpostId", 20)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            validator.validate(event.journalpostId)
        }
    }

    private object LinkSoknadValidator: SoknadOpprettetValidator {
        override val description = LinkContentValidator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            LinkContentValidator.validate(event.linkSoknad)
        }
    }

    private object MottatteVedleggTittelValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Tittel for mottatte vedlegg", 255)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.mottatteVedlegg.all {
                validator.validate(it.tittel)
            }
        }
    }

    private object EtterspurteVedleggTittelValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Tittel for etterspurte vedlegg", 255)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.etterspurteVedlegg.all {
                validator.validate(it.tittel)
            }
        }
    }

    private object EtterspurteVedleggBeskrivelseValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("Beskrivelse for etterspurte vedlegg", 500)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.etterspurteVedlegg.all {
                validator.validate(it.tittel)
            }
        }
    }


    private object VedleggsIdLengthValidator: SoknadOpprettetValidator {
        private val validator = TextLengthValidator("VedleggsId", 40)

        override val description = validator.description

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.mottatteVedlegg.all { validator.validate(it.vedleggsId) } &&
                event.etterspurteVedlegg.all { validator.validate(it.vedleggsId) }
        }
    }

    private object VedleggAlleredeMottattValidator: SoknadOpprettetValidator {
        override val description = "Kan ikke si at et etterspurt vedlegg er mottatt i samme event"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.mottatteVedlegg.map { it.vedleggsId }.none { vedleggsId ->
                event.etterspurteVedlegg.map { it.vedleggsId }.contains(vedleggsId)
            }
        }
    }

    private object VedleggsIdDuplicateValidator: SoknadOpprettetValidator {
        override val description = "Kan ikke gjenbruke samme vedleggsId flere ganger for én søknad"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.mottatteVedlegg.map { it.vedleggsId }.let { it.size == it.distinct().size } &&
                event.etterspurteVedlegg.map { it.vedleggsId }.let { it.size == it.distinct().size }
        }
    }

    private object MottatteVedleggLinkContentValidator: SoknadOpprettetValidator {
        override val description = "Feil i link til mottatt vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.mottatteVedlegg.all {
                LinkContentValidator.validate(it.linkVedlegg)
            }
        }
    }

    private object EtterspurteVedleggLinkContentValidator: SoknadOpprettetValidator {
        override val description = "Feil i link for å ettersende vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.etterspurteVedlegg.all {
                LinkContentValidator.validate(it.linkEttersending)
            }
        }
    }

    private object BrukerErAvsenderValidator: SoknadOpprettetValidator {
        override val description = "Link til ettersending er påkrevd dersom bruker skal sende inn"

        override fun validate(event: SoknadOpprettet) = assertTrue {
            event.etterspurteVedlegg.all {
                if (it.brukerErAvsender) {
                    it.linkEttersending != null
                } else {
                    true
                }
            }
        }
    }
}
