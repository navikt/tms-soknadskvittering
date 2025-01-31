package no.nav.tms.soknadskvittering.api

import java.time.LocalDate
import java.time.ZonedDateTime

data class SoknadsKvitteringDto(
    val soknadsId: String,
    val tittel: String,
    val tema: String,
    val skjemanummer: String,
    val mottattTidspunkt: ZonedDateTime,
    val fristEttersending: LocalDate,
    val linkSoknad: String?,
    val journalpost: String?,
    val vedleggMottatt: List<MottattVedleggDto>,
    val vedleggMangler: List<EtterspurtVedleggDto>,
    val opprettet: ZonedDateTime
)

data class MottattVedleggDto(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val linkVedlegg: String?
)

data class EtterspurtVedleggDto(
    val vedleggsId: String,
    val brukerErAvsender: Boolean,
    val tittel: String,
    val beskrivelse: String?,
    val linkEttersending: String?
)
