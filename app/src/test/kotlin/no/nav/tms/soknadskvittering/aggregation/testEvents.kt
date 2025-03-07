package no.nav.tms.soknadskvittering.aggregation

import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.nowAtUtc
import java.time.LocalDate
import java.time.ZonedDateTime

fun innsendtEvent(
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
    produsent: String = produsentJson(),
    metadata: String? = null,
    eventName: String = "soknadInnsendt"
) = """
{
    "@event_name": "$eventName",
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
    "etterspurteVedlegg": ${etterspurteVedlegg.joinToString(prefix = "[", postfix = "]")},
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
} 
"""

fun oppdatertEvent(
    soknadsId: String,
    fristEttersending: LocalDate? = null,
    linkSoknad: String? = null,
    journalpostId: String? = null,
    produsent: String = produsentJson(),
    metadata: String? = null
) = """
{
    "@event_name": "soknadOppdatert",
    "soknadsId": "$soknadsId",
    "fristEttersending": ${fristEttersending?.toString().asJson()}, 
    "linkSoknad": ${linkSoknad.asJson()}, 
    "journalpostId": ${journalpostId.asJson()},
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
}
"""

fun ferdigstiltEvent(
    soknadsId: String,
    produsent: String = produsentJson(),
    metadata: String? = null
) = """
{
    "@event_name": "soknadFerdigstilt",
    "soknadsId": "$soknadsId",
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
}
"""

fun vedleggEtterspurtEvent(
    soknadsId: String,
    vedleggsId: String,
    brukerErAvsender: Boolean = true,
    tittel: String = "tittel",
    linkEttersending: String? = "https://link.til.ettersending",
    beskrivelse: String? = null,
    tidspunktEtterspurt: ZonedDateTime = nowAtUtc(),
    produsent: String = produsentJson(),
    metadata: String? = null
) = """
{
    "@event_name": "vedleggEtterspurt",
    "soknadsId": "$soknadsId",
    "vedleggsId": "$vedleggsId",
    "brukerErAvsender": $brukerErAvsender,
    "tittel": "$tittel",
    "linkEttersending": ${linkEttersending.asJson()},
    "beskrivelse": ${beskrivelse.asJson()},
    "tidspunktEtterspurt": "$tidspunktEtterspurt",
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
}
"""

fun vedleggMottattEvent(
    soknadsId: String,
    vedleggsId: String,
    tittel: String = "tittel",
    brukerErAvsender: Boolean = true,
    linkVedlegg: String? = "https://link.til.vedlegg",
    journalpostId: String? = "123456",
    tidspunktMottatt: ZonedDateTime = nowAtUtc(),
    produsent: String = produsentJson(),
    metadata: String? = null
) = """
{
    "@event_name": "vedleggMottatt",
    "soknadsId": "$soknadsId",
    "vedleggsId": "$vedleggsId",
    "brukerErAvsender": $brukerErAvsender,
    "tittel": "$tittel",
    "linkVedlegg": ${linkVedlegg.asJson()},
    "journalpostId": ${journalpostId.asJson()},
    "tidspunktMottatt": "$tidspunktMottatt",
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
}
"""

fun vedleggOppdatertEvent(
    soknadsId: String,
    vedleggsId: String,
    linkVedlegg: String? = "https://link.til.vedlegg",
    journalpostId: String? = "123456",
    produsent: String = produsentJson(),
    metadata: String? = null
) = """
{
    "@event_name": "vedleggOppdatert",
    "soknadsId": "$soknadsId",
    "vedleggsId": "$vedleggsId",
    "linkVedlegg": ${linkVedlegg.asJson()},
    "journalpostId": ${journalpostId.asJson()},
    "produsent": $produsent,
    "metadata": ${metadata.asJson()}
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
    linkVedlegg: String? = "https://link.til.vedlegg",
    journalpostId: String? = "123456"
) =
    """
{
    "vedleggsId": "$vedleggsId",
    "tittel": "$tittel",
    "linkVedlegg": ${linkVedlegg.asJson()},
    "journalpostId": ${journalpostId.asJson()}
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

fun produsentJson(
    cluster: String = "cluster",
    namespace: String = "namespace",
    appnavn: String = "appnavn"
) = """
{
    "cluster": "$cluster",
    "namespace": "$namespace",
    "appnavn": "$appnavn"
}
"""

