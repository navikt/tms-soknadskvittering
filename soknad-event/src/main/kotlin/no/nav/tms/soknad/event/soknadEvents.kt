package no.nav.tms.soknad.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.ZonedDateTime

data class SoknadOpprettet(
    val soknadsId: String,
    val ident: String,
    val tittel: String,
    val temakode: String,
    val skjemanummer: String,
    val tidspunktMottatt: ZonedDateTime,
    val fristEttersending: LocalDate,
    val linkSoknad: String?,
    val journalpostId: String?,
    val mottatteVedlegg: List<MottattVedlegg>,
    val etterspurteVedlegg: List<EtterspurtVedlegg>
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
    val beskrivelse: String?,
    val tidspunktEtterspurt: ZonedDateTime
) {
    @JsonProperty("@event_name") val eventName = "vedlegg_etterspurt"
}

data class VedleggMottatt(
    val soknadsId: String,
    val vedleggsId: String,
    val tittel: String,
    val linkVedlegg: String?,
    val brukerErAvsender: Boolean,
    val tidspunktMottatt: ZonedDateTime
) {
    @JsonProperty("@event_name") val eventName = "vedlegg_mottatt"
}

data class MottattVedlegg(
    val vedleggsId: String,
    val tittel: String,
    val linkVedlegg: String
)

data class EtterspurtVedlegg(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val beskrivelse: String?,
    val linkEttersending: String?
)
