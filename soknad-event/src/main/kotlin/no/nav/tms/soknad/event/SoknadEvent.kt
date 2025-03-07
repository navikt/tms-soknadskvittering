package no.nav.tms.soknad.event

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.ZonedDateTime

object SoknadEvent {
    const val version = "v0.1.1"

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
        val mottatteVedlegg: List<Dto.MottattVedlegg>,
        val etterspurteVedlegg: List<Dto.EtterspurtVedlegg>,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "soknadOpprettet"
    }

    data class SoknadOppdatert(
        val soknadsId: String,
        val fristEttersending: LocalDate?,
        val linkSoknad: String?,
        val journalpostId: String?,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "soknadOppdatert"
    }

    data class SoknadFerdigstilt(
        val soknadsId: String,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "soknadFerdigstilt"
    }

    data class VedleggEtterspurt(
        val soknadsId: String,
        val vedleggsId: String,
        val brukerErAvsender: Boolean,
        val tittel: String,
        val linkEttersending: String?,
        val beskrivelse: String?,
        val tidspunktEtterspurt: ZonedDateTime,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "vedleggEtterspurt"
    }

    data class VedleggMottatt(
        val soknadsId: String,
        val vedleggsId: String,
        val tittel: String,
        val linkVedlegg: String?,
        val brukerErAvsender: Boolean,
        val tidspunktMottatt: ZonedDateTime,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "vedleggMottatt"
    }

    data class VedleggOppdatert(
        val soknadsId: String,
        val vedleggsId: String,
        val linkVedlegg: String?,
        val produsent: Dto.Produsent,
        val metadata: Map<String, Any>?
    ) {
        @JsonProperty("@event_name") val eventName = "vedleggOppdatert"
    }

    object Dto {
        data class Produsent(
            val cluster: String,
            val namespace: String,
            val appnavn: String
        )

        data class MottattVedlegg(
            val vedleggsId: String,
            val tittel: String,
            val linkVedlegg: String?
        )

        data class EtterspurtVedlegg(
            val vedleggsId: String,
            val brukerErAvsender: Boolean,
            val tittel: String,
            val beskrivelse: String?,
            val linkEttersending: String?
        )
    }
}


