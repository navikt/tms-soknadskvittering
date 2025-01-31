package no.nav.tms.soknadskvittering.subscribers

import no.nav.tms.soknadskvittering.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.Vedlegg
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.nowAtUtc
import java.time.LocalDate
import java.time.ZonedDateTime

fun opprettEvent(
    soknadsId: String,
    ident: String = "12345678910",
    tittel: String = "tittel",
    temakode: String = "KOD",
    skjemanummer: String = "skjemanummer",
    mottattTidspunkt: ZonedDateTime = nowAtUtc(),
    fristEttersending: LocalDate = LocalDate.now().plusDays(14),
    linkSoknad: String? = "https://link.til.soknad",
    journalpostId: String? = "journalpostId",
    vedlegg: List<Vedlegg> = emptyList(),
    etterspurteVedlegg: List<EtterspurtVedlegg> = emptyList(),
) = """
{
    "@event_name": "soknad_opprettet",
    "soknadsId": "$soknadsId",
    "ident": "$ident",
    "tittel": "$tittel",
    "temakode": "$temakode",
    "skjemanummer": "$skjemanummer",
    "mottattTidspunkt": "$mottattTidspunkt",
    "fristEttersending": "$fristEttersending",
    "linkSoknad": ${linkSoknad.asJson()},
    "journalpostId": ${journalpostId.asJson()},
    "vedlegg": ${vedleggJson(vedlegg)},
    "etterspurteVedlegg": ${etterspurteVedleggJson(etterspurteVedlegg)}
} 
"""

private fun Any?.asJson() = if (this == null) {
    "null"
} else if (this is String) {
    "\"$this\""
} else {
    "$this"
}

private fun vedleggJson(vedlegg: List<Vedlegg>): String = vedlegg.joinToString(prefix = "[", postfix = "]") {
    """
{
    "vedleggsId": "${it.vedleggsId}",
    "tittel": "${it.tittel}",
    "linkVedlegg": ${it.linkVedlegg}
} 
    """
}

private fun etterspurteVedleggJson(etterspurteVedlegg: List<EtterspurtVedlegg>): String = etterspurteVedlegg
    .joinToString(prefix = "[", postfix = "]") {
    """
{
    "vedleggsId": "${it.vedleggsId}",
    "brukerErAvsender": "${it.brukerErAvsender}",
    "tittel": "${it.tittel}"
    "beskrivelse": ${it.beskrivelse.asJson()}
    "linkEttersending": ${it.linkEttersending.asJson()}
} 
    """
}
