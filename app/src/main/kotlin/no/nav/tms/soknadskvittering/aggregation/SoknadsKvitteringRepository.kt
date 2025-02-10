package no.nav.tms.soknadskvittering.aggregation

import kotliquery.queryOf
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.SoknadsKvittering
import no.nav.tms.soknadskvittering.aggregation.DatabaseDto.MottattVedlegg
import no.nav.tms.soknadskvittering.setup.*
import java.time.LocalDate

class SoknadsKvitteringRepository(private val database: Database) {

    private val objectMapper = defaultObjectMapper()

    fun insertSoknadsKvittering(soknadsKvittering: SoknadsKvittering): Boolean {
        return database.insert {
            queryOf(
                """
                    insert into soknadskvittering(
                        soknadsId,
                        ident,
                        tittel,
                        temakode,
                        skjemanummer,
                        tidspunktMottatt,
                        fristEttersending,
                        linkSoknad,
                        journalpostId,
                        mottatteVedlegg,
                        etterspurteVedlegg,
                        produsent,
                        opprettet,
                        ferdigstilt
                    ) values (
                        :soknadsId,
                        :ident,
                        :tittel,
                        :temakode,
                        :skjemanummer,
                        :tidspunktMottatt,
                        :fristEttersending,
                        :linkSoknad,
                        :journalpostId,
                        :mottatteVedlegg,
                        :etterspurteVedlegg,
                        :produsent,
                        :opprettet,
                        :ferdigstilt
                    ) on conflict do nothing
                """, mapOf(
                    "soknadsId" to soknadsKvittering.soknadsId,
                    "ident" to soknadsKvittering.ident,
                    "tittel" to soknadsKvittering.tittel,
                    "temakode" to soknadsKvittering.temakode,
                    "skjemanummer" to soknadsKvittering.skjemanummer,
                    "tidspunktMottatt" to soknadsKvittering.tidspunktMottatt,
                    "fristEttersending" to soknadsKvittering.fristEttersending,
                    "linkSoknad" to soknadsKvittering.linkSoknad,
                    "journalpostId" to soknadsKvittering.journalpostId,
                    "mottatteVedlegg" to soknadsKvittering.mottatteVedlegg.toJsonb(objectMapper),
                    "etterspurteVedlegg" to soknadsKvittering.etterspurteVedlegg.toJsonb(objectMapper),
                    "produsent" to soknadsKvittering.produsent.toJsonb(objectMapper),
                    "opprettet" to soknadsKvittering.opprettet,
                    "ferdigstilt" to soknadsKvittering.ferdigstilt
                )
            )
        }
    }

    fun updateSoknadsKvittering(
        soknadsId: String,
        fristEttersending: LocalDate?,
        linkSoknad: String?,
        journalpostId: String?,
    ): Boolean {
        return 0 < database.update {
            queryOf("""
                update
                    soknadskvittering
                set
                    fristEttersending = coalesce(:fristEttersending, fristEttersending),
                    linkSoknad = coalesce(:linkSoknad, linkSoknad),
                    journalpostId = coalesce(:journalpostId, journalpostId)
                where
                    soknadsId = :soknadsId
            """, mapOf(
                "soknadsId" to soknadsId,
                "fristEttersending" to fristEttersending,
                "linkSoknad" to linkSoknad,
                "journalpostId" to journalpostId,
            ))
        }
    }

    fun getSoknadsKvittering(soknadsId: String): SoknadsKvittering? {
        return database.singleOrNull {
            queryOf("""
                select 
                    soknadsId,
                    ident,
                    tittel,
                    temakode,
                    skjemanummer,
                    tidspunktMottatt,
                    fristEttersending,
                    linkSoknad,
                    journalpostId,
                    mottatteVedlegg,
                    etterspurteVedlegg,
                    produsent,
                    opprettet,
                    ferdigstilt
                from 
                    soknadskvittering
                where 
                    soknadsId = :soknadsId
            """, mapOf(
                "soknadsId" to soknadsId
            )).map {
                SoknadsKvittering(
                    soknadsId = it.string("soknadsId"),
                    ident = it.string("ident"),
                    tittel = it.string("tittel"),
                    temakode = it.string("temakode"),
                    skjemanummer = it.string("skjemanummer"),
                    tidspunktMottatt = it.zonedDateTime("tidspunktMottatt"),
                    fristEttersending = it.localDate("fristEttersending"),
                    linkSoknad = it.stringOrNull("linkSoknad"),
                    journalpostId = it.stringOrNull("journalpostId"),
                    mottatteVedlegg = it.json("mottatteVedlegg", objectMapper),
                    etterspurteVedlegg = it.json("etterspurteVedlegg", objectMapper),
                    produsent = it.json("produsent", objectMapper),
                    opprettet = it.zonedDateTime("opprettet"),
                    ferdigstilt = it.zonedDateTimeOrNull("ferdigstilt")
                )
            }.asSingle
        }
    }

    fun getSoknadskvitteringForUser(ident: String): List<SoknadsKvittering> {
        return database.list {
            queryOf("""
                select 
                    soknadsId,
                    ident,
                    tittel,
                    temakode,
                    skjemanummer,
                    tidspunktMottatt,
                    fristEttersending,
                    linkSoknad,
                    journalpostId,
                    mottatteVedlegg,
                    etterspurteVedlegg,
                    produsent,
                    opprettet,
                    ferdigstilt
                from 
                    soknadskvittering
                where 
                    ident = :ident
            """, mapOf(
                "ident" to ident
            )).map {
                SoknadsKvittering(
                    soknadsId = it.string("soknadsId"),
                    ident = it.string("ident"),
                    tittel = it.string("tittel"),
                    temakode = it.string("temakode"),
                    skjemanummer = it.string("skjemanummer"),
                    tidspunktMottatt = it.zonedDateTime("tidspunktMottatt"),
                    fristEttersending = it.localDate("fristEttersending"),
                    linkSoknad = it.stringOrNull("linkSoknad"),
                    journalpostId = it.stringOrNull("journalpostId"),
                    mottatteVedlegg = it.json("mottatteVedlegg", objectMapper),
                    etterspurteVedlegg = it.json("etterspurteVedlegg", objectMapper),
                    produsent = it.json("produsent", objectMapper),
                    opprettet = it.zonedDateTime("opprettet"),
                    ferdigstilt = it.zonedDateTimeOrNull("ferdigstilt")
                )
            }.asList
        }
    }

    fun markerFerdigstilt(soknadsId: String): Boolean {
        return 0 < database.update {
            queryOf(
                "update soknadsKvittering set ferdigstilt = :now where soknadsId = :soknadsId",
                mapOf("soknadsId" to soknadsId, "now" to ZonedDateTimeHelper.nowAtUtc())
            )
        }
    }

    fun oppdaterMottatteVedlegg(soknadsId: String, mottatteVedlegg: List<MottattVedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        mottatteVedlegg = :mottatteVedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "mottatteVedlegg" to mottatteVedlegg.toJsonb(objectMapper),
                )
            )
        }
    }

    fun oppdaterEtterspurteVedlegg(soknadsId: String, etterspurteVedlegg: List<EtterspurtVedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        etterspurteVedlegg = :etterspurteVedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "etterspurteVedlegg" to etterspurteVedlegg.toJsonb(objectMapper),
                )
            )
        }
    }

    fun oppdaterAlleVedlegg(soknadsId: String, mottatteVedlegg: List<MottattVedlegg>, etterspurteVedlegg: List<EtterspurtVedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        mottatteVedlegg = :mottatteVedlegg,
                        etterspurteVedlegg = :etterspurteVedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "mottatteVedlegg" to mottatteVedlegg.toJsonb(objectMapper),
                    "etterspurteVedlegg" to etterspurteVedlegg.toJsonb(objectMapper)
                )
            )
        }
    }
}
