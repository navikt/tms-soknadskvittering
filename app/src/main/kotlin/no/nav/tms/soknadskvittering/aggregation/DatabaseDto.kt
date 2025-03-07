package no.nav.tms.soknadskvittering.aggregation

import java.time.LocalDate
import java.time.ZonedDateTime

object DatabaseDto {
    data class SoknadsKvittering(
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
        val etterspurteVedlegg: List<EtterspurtVedlegg>,
        val produsent: Produsent,
        val opprettet: ZonedDateTime,
        val ferdigstilt: ZonedDateTime?
    )

    data class MottattVedlegg(
        val vedleggsId: String,
        val brukerErAvsender: Boolean,
        val erEttersending: Boolean,
        val tittel: String,
        val linkVedlegg: String?,
        val journalpostId: String?,
        val tidspunktMottatt: ZonedDateTime
    )

    data class EtterspurtVedlegg(
        val vedleggsId: String,
        val brukerErAvsender: Boolean,
        val tittel: String,
        val linkEttersending: String?,
        val beskrivelse: String?,
        val tidspunktEtterspurt: ZonedDateTime,
        val erMottatt: Boolean
    )

    data class Produsent(
        val cluster: String,
        val namespace: String,
        val appnavn: String
    )
}
