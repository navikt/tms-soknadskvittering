package no.nav.tms.soknadskvittering.soknad

import java.time.LocalDate
import java.time.ZonedDateTime

data class SoknadsKvittering (
    val soknadsId: String,
    val tittel: String,
    val tema: String,
    val skjemanummer: String,
    val mottatt: ZonedDateTime,
    val fristEttersending: LocalDate,
    val linkSoknad: String?,
    val journalpost: String?,
    val vedleggMottatt: List<MottattVedlegg>,
    val vedleggMangler: List<EtterspurtVedlegg>,
    val opprettet: ZonedDateTime
)

data class MottattVedlegg(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
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
