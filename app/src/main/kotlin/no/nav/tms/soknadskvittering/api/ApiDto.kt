package no.nav.tms.soknadskvittering.api

import no.nav.tms.soknadskvittering.aggregation.DatabaseDto
import java.time.LocalDate
import java.time.ZonedDateTime

object ApiDto {
    data class SoknadsKvitteringHeader(
        val soknadsId: String,
        val tittel: String,
        val temakode: String,
        val tidspunktMottatt: ZonedDateTime,
        val fristEttersending: LocalDate
    )

    data class SoknadsKvittering(
        val soknadsId: String,
        val tittel: String,
        val temakode: String,
        val skjemanummer: String,
        val tidspunktMottatt: ZonedDateTime,
        val fristEttersending: LocalDate,
        val linkSoknad: String?,
        val journalpostId: String?,
        val mottatteVedlegg: List<MottattVedlegg>,
        val manglendeVedlegg: List<ManglendeVedlegg>,
        val opprettet: ZonedDateTime
    )

    data class ManglendeVedlegg(
        val vedleggsId: String,
        val brukerErAvsender: Boolean,
        val tittel: String,
        val beskrivelse: String?,
        val linkEttersending: String?,
        val tidspunktEtterspurt: ZonedDateTime
    )

    data class MottattVedlegg(
        val vedleggsId: String,
        val brukerErAvsender: Boolean,
        val erEttersending: Boolean,
        val tittel: String,
        val linkVedlegg: String?,
        val tidspunktMottatt: ZonedDateTime
    )

    fun mapSoknadsKvittering(kvittering: DatabaseDto.SoknadsKvittering) = SoknadsKvittering(
        soknadsId = kvittering.soknadsId,
        tittel = kvittering.tittel,
        temakode = kvittering.temakode,
        skjemanummer = kvittering.skjemanummer,
        tidspunktMottatt = kvittering.tidspunktMottatt,
        fristEttersending = kvittering.fristEttersending,
        linkSoknad = kvittering.linkSoknad,
        journalpostId = kvittering.journalpostId,
        mottatteVedlegg = kvittering.mottatteVedlegg
            .map(::mottattVedlegg),
        manglendeVedlegg = kvittering.etterspurteVedlegg
            .filterNot { it.erMottatt }
            .map(::manglendeVedlegg),
        opprettet = kvittering.opprettet,
    )

    fun mapSoknadsKvitteringHeader(kvittering: DatabaseDto.SoknadsKvittering) = SoknadsKvitteringHeader(
        soknadsId = kvittering.soknadsId,
        tittel = kvittering.tittel,
        temakode = kvittering.temakode,
        tidspunktMottatt = kvittering.tidspunktMottatt,
        fristEttersending = kvittering.fristEttersending
    )

    private fun manglendeVedlegg(vedlegg: DatabaseDto.EtterspurtVedlegg) = ManglendeVedlegg (
        vedleggsId = vedlegg.vedleggsId,
        brukerErAvsender = vedlegg.brukerErAvsender,
        tittel = vedlegg.tittel,
        beskrivelse = vedlegg.beskrivelse,
        linkEttersending = vedlegg.linkEttersending,
        tidspunktEtterspurt = vedlegg.tidspunktEtterspurt,
    )

    private fun mottattVedlegg(vedlegg: DatabaseDto.MottattVedlegg) = MottattVedlegg(
        vedleggsId = vedlegg.vedleggsId,
        brukerErAvsender = vedlegg.brukerErAvsender,
        erEttersending = vedlegg.erEttersending,
        tittel = vedlegg.tittel,
        linkVedlegg = vedlegg.linkVedlegg,
        tidspunktMottatt = vedlegg.tidspunktMottatt
    )
}
