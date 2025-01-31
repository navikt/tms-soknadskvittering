package no.nav.tms.soknadskvittering

import java.time.LocalDate
import java.time.ZonedDateTime

data class SoknadsKvittering(
    val soknadsId: String,
    val ident: String,
    val tittel: String,
    val temakode: String,
    val skjemanummer: String,
    val mottattTidspunkt: ZonedDateTime,
    val fristEttersending: LocalDate,
    val linkSoknad: String?,
    val journalpostId: String?,
    val vedlegg: List<Vedlegg>,
    val etterspurteVedlegg: List<EtterspurtVedlegg>,
    val opprettet: ZonedDateTime,
    val ferdigstilt: ZonedDateTime?,
)

data class Vedlegg(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val linkVedlegg: String?,
    val tidspunktMottatt: ZonedDateTime
)

data class EtterspurtVedlegg(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val linkEttersending: String?,
    val beskrivelse: String?,
    val tidspunktEtterspurt: ZonedDateTime
)
