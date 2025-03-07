package no.nav.tms.soknad.event.validation
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.tms.soknad.event.SoknadEvent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class SoknadInnsendtValidationTest {

    private val validEvent = soknadInnsendt()

    private val text10Chars = "Laaaaaaang"

    @Test
    fun `godkjenner gyldig innsendt-event`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            SoknadInnsendtValidation.validate(validEvent)
        }
    }

    @Test
    fun `feiler hvis soknadsId er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                soknadsId = "badId"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis ident er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                ident = "badIdent"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis tittel er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                tittel = "${text10Chars.repeat(8)}+"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis temakode er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                temakode = "LANG"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis skjemanummer er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                skjemanummer = "${text10Chars.repeat(4)}+"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis journalpostId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                journalpostId = "${text10Chars.repeat(2)}+"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link til søknad er ugyldig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkSoknad = "badLink"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                linkSoknad = "https://${text10Chars.repeat(20)}"
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis mottatt vedleggsId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        vedleggsId = "${text10Chars.repeat(4)}+"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis vedleggsId er duplikat`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        vedleggsId = "sammeId"
                    ),
                    mottattVedlegg(
                        vedleggsId = "sammeId"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis vedlegg er mottatt og etterspurt samtidig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        vedleggsId = "vedlegg-123"
                    )
                ),
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        vedleggsId = "vedlegg-123"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis mottatt vedleggs tittel er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        tittel = "${text10Chars.repeat(25)}++++++"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis mottatt vedleggs link er feilaktig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        linkVedlegg = "badLink"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis mottatt vedleggs link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        linkVedlegg = "https://${text10Chars.repeat(20)}"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis mottatt vedleggs journalpostId er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                mottatteVedlegg = listOf(
                    mottattVedlegg(
                        journalpostId = "${text10Chars.repeat(2)}+"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis etterspurt vedleggs tittel er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        tittel = "${text10Chars.repeat(25)}++++++"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis etterspurt vedleggs beskrivelse er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        beskrivelse = "${text10Chars.repeat(50)}+"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis etterspurt vedleggs link er feilaktig`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        linkEttersending = "badLink"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler hvis etterspurt vedleggs link er for lang`() {
        shouldThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        linkEttersending = "https://${text10Chars.repeat(20)}"
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }

    @Test
    fun `feiler ikke hvis bruker er avsender av etterspurt vedlegg og link mangler`() {
        shouldNotThrow<SoknadskvitteringValidationException> {
            validEvent.copy(
                etterspurteVedlegg = listOf(
                    etterspurtVedlegg(
                        brukerErAvsender = true,
                        linkEttersending = null
                    )
                )
            ).let {
                SoknadInnsendtValidation.validate(it)
            }
        }
    }


    private fun soknadInnsendt() = SoknadEvent.SoknadInnsendt(
        soknadsId = UUID.randomUUID().toString(),
        ident = "01234567890",
        tittel = "Tittel på søknad",
        temakode = "UKJ",
        skjemanummer = "skjema-123",
        tidspunktMottatt = ZonedDateTime.now(),
        fristEttersending = LocalDate.now().plusDays(14),
        linkSoknad = "https://link.til.soknad",
        journalpostId = "123456",
        mottatteVedlegg = listOf(mottattVedlegg()),
        etterspurteVedlegg = listOf(etterspurtVedlegg()),
        produsent = SoknadEvent.Dto.Produsent("dev", "team", "app"),
        metadata = mapOf("meta" to "data"),
    )

    fun mottattVedlegg(
        vedleggsId: String = "vedlegg-1",
        tittel: String = "Tittel på vedlegg",
        linkVedlegg: String? = "https://link.til.vedlegg",
        journalpostId: String? = "123456"
    ) = SoknadEvent.Dto.MottattVedlegg(
        vedleggsId = vedleggsId,
        tittel = tittel,
        linkVedlegg = linkVedlegg,
        journalpostId = journalpostId
    )

    fun etterspurtVedlegg(
        vedleggsId: String = "vedlegg-2",
        tittel: String = "Tittel på etterspurt vedlegg",
        beskrivelse: String? = "Ledetekst",
        brukerErAvsender: Boolean = false,
        linkEttersending: String? = null
    ) = SoknadEvent.Dto.EtterspurtVedlegg(
        vedleggsId = vedleggsId,
        tittel = tittel,
        beskrivelse = beskrivelse,
        brukerErAvsender = brukerErAvsender,
        linkEttersending = linkEttersending
    )
}
