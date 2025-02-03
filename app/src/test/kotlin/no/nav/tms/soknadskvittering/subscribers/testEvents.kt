package no.nav.tms.soknadskvittering.subscribers

import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.nowAtUtc
import java.time.LocalDate
import java.time.ZonedDateTime

fun opprettetEvent(
    soknadsId: String,
    ident: String = "12345678910",
    tittel: String = "tittel",
    temakode: String = "KOD",
    skjemanummer: String = "skjemanummer",
    tidspunktMottatt: ZonedDateTime = nowAtUtc(),
    fristEttersending: LocalDate = LocalDate.now().plusDays(14),
    linkSoknad: String? = "https://link.til.soknad",
    journalpostId: String? = "journalpostId",
    mottatteVedlegg: List<String> = emptyList(),
    etterspurteVedlegg: List<String> = emptyList(),
) = """
{
    "@event_name": "soknad_opprettet",
    "soknadsId": "$soknadsId",
    "ident": "$ident",
    "tittel": "$tittel",
    "temakode": "$temakode",
    "skjemanummer": "$skjemanummer",
    "tidspunktMottatt": "$tidspunktMottatt",
    "fristEttersending": "$fristEttersending",
    "linkSoknad": ${linkSoknad.asJson()},
    "journalpostId": ${journalpostId.asJson()},
    "mottatteVedlegg": ${mottatteVedlegg.joinToString(prefix = "[", postfix = "]")},
    "etterspurteVedlegg": ${etterspurteVedlegg.joinToString(prefix = "[", postfix = "]")}
} 
"""

fun oppdatertEvent(
    soknadsId: String,
    fristEttersending: LocalDate? = null,
    linkSoknad: String? = null,
    journalpostId: String? = null
) = """
{
    "@event_name": "soknad_oppdatert",
    "soknadsId": "$soknadsId",
    "fristEttersending": ${fristEttersending?.toString().asJson()}, 
    "linkSoknad": ${linkSoknad.asJson()}, 
    "journalpostId": ${journalpostId.asJson()} 
}
"""

private fun Any?.asJson() = if (this == null) {
    "null"
} else if (this is String) {
    "\"$this\""
} else {
    "$this"
}

fun mottattVedleggJson(
    vedleggsId: String,
    tittel: String = "Navn på vedlegg",
    linkVedlegg: String = "https://link.til.vedlegg"
) =
    """
{
    "vedleggsId": "$vedleggsId",
    "tittel": "$tittel",
    "linkVedlegg": "$linkVedlegg"
} 
    """

fun etterspurteVedleggJson(
    vedleggsId: String,
    brukerErAvsender: Boolean = true,
    tittel: String = "Navn på vedlegg",
    beskrivelse: String? = null,
    linkEttersending: String? = null
): String =
    """
{
    "vedleggsId": "$vedleggsId",
    "brukerErAvsender": "$brukerErAvsender",
    "tittel": "$tittel",
    "beskrivelse": ${beskrivelse.asJson()},
    "linkEttersending": ${linkEttersending.asJson()}
} 
    """
