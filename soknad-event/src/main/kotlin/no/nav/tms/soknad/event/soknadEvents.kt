package no.nav.tms.soknad.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.reflect.full.functions

data class SoknadOpprettet(
    val soknadsId: String,
    val ident: String,
    val tittel: String,
    val temakode: String,
    val skjemanummer: String,
    val mottatt: ZonedDateTime,
    val fristEttersending: LocalDate,
    val linkSoknad: String?,
    val journalpostId: String?,
    val vedlegg: List<Vedlegg>
) {
    @JsonProperty("@event_name") val eventName = "soknad_opprettet"
}

data class SoknadOppdatert(
    val soknadsId: String,
    val fristEttersending: LocalDate?,
    val linkSoknad: String?,
    val journalpostId: String?
) {
    @JsonProperty("@event_name") val eventName = "soknad_oppdatert"
}

data class SoknadFerdigstilt(
    val soknadsId: String
) {
    @JsonProperty("@event_name") val eventName = "soknad_ferdigstilt"
}

data class VedleggEtterspurt(
    val soknadsId: String,
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val linkEttersending: String?,
    val beskrivelse: String?
) {
    @JsonProperty("@event_name") val eventName = "vedlegg_etterspurt"
}

data class VedleggMottatt(
    val soknadsId: String,
    val vedleggsId: String,
    val tittel: String?,
    val linkVedlegg: String?,
    val tidspunktMottatt: ZonedDateTime
) {
    @JsonProperty("@event_name") val eventName = "vedlegg_mottatt"
}

data class Vedlegg(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val erMottatt: Boolean,
    val tittel: String,
    val linkVedlegg: String?,
    val linkEttersending: String?,
    val beskrivelse: String?,
    val tidspunktMottatt: ZonedDateTime
)
