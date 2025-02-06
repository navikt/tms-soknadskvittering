package no.nav.tms.soknadskvittering.subscribers

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.StringBody
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

fun ferdigstiltEvent(
    soknadsId: String
) = """
{
    "@event_name": "soknad_ferdigstilt",
    "soknadsId": "$soknadsId"
}
"""

fun vedleggEtterspurtEvent(
    soknadsId: String,
    vedleggsId: String,
    brukerErAvsender: Boolean = true,
    tittel: String = "tittel",
    linkEttersending: String? = "https://link.til.ettersending",
    beskrivelse: String? = null,
    tidspunktEtterspurt: ZonedDateTime = nowAtUtc()
) = """
{
    "@event_name": "vedlegg_etterspurt",
    "soknadsId": "$soknadsId",
    "vedleggsId": "$vedleggsId",
    "brukerErAvsender": $brukerErAvsender,
    "tittel": "$tittel",
    "linkEttersending": ${linkEttersending.asJson()},
    "beskrivelse": ${beskrivelse.asJson()},
    "tidspunktEtterspurt": "$tidspunktEtterspurt"
}
"""

fun vedleggMottattEvent(
    soknadsId: String,
    vedleggsId: String,
    tittel: String = "tittel",
    brukerErAvsender: Boolean = true,
    linkVedlegg: String? = "https://link.til.vedlegg",
    tidspunktMottatt: ZonedDateTime = nowAtUtc()
) = """
{
    "@event_name": "vedlegg_mottatt",
    "soknadsId": "$soknadsId",
    "vedleggsId": "$vedleggsId",
    "brukerErAvsender": $brukerErAvsender,
    "tittel": "$tittel",
    "linkVedlegg": ${linkVedlegg.asJson()},
    "tidspunktMottatt": "$tidspunktMottatt"
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

fun etterspurtVedleggJson(
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
