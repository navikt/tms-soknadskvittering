package no.nav.tms.soknadskvittering.subscribers

import kotliquery.queryOf
import no.nav.tms.soknadskvittering.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.SoknadsKvittering
import no.nav.tms.soknadskvittering.Vedlegg
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
                        mottattTidspunkt,
                        fristEttersending,
                        linkSoknad,
                        journalpostId,
                        vedlegg,
                        etterspurteVedlegg,
                        opprettet,
                        ferdigstilt
                    ) values (
                        :soknadsId,
                        :ident,
                        :tittel,
                        :temakode,
                        :skjemanummer,
                        :mottattTidspunkt,
                        :fristEttersending,
                        :linkSoknad,
                        :journalpostId,
                        :vedlegg,
                        :etterspurteVedlegg,
                        :opprettet,
                        :ferdigstilt
                    ) on conflict do nothing
                """, mapOf(
                    "soknadsId" to soknadsKvittering.soknadsId,
                    "ident" to soknadsKvittering.ident,
                    "tittel" to soknadsKvittering.tittel,
                    "temakode" to soknadsKvittering.temakode,
                    "skjemanummer" to soknadsKvittering.skjemanummer,
                    "mottattTidspunkt" to soknadsKvittering.mottattTidspunkt,
                    "fristEttersending" to soknadsKvittering.fristEttersending,
                    "linkSoknad" to soknadsKvittering.linkSoknad,
                    "journalpostId" to soknadsKvittering.journalpostId,
                    "vedlegg" to soknadsKvittering.vedlegg.toJsonb(objectMapper),
                    "etterspurteVedlegg" to soknadsKvittering.etterspurteVedlegg.toJsonb(objectMapper),
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
                    fristEttersending = coalesce(fristEttersending, :fristEttersending),
                    linkSoknad = coalesce(linkSoknad, :linkSoknad),
                    journalpostId = coalesce(journalpostId, :journalpostId)
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
                    mottattTidspunkt,
                    fristEttersending,
                    linkSoknad,
                    journalpostId,
                    vedlegg,
                    etterspurteVedlegg,
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
                    mottattTidspunkt = it.zonedDateTime("mottattTidspunkt"),
                    fristEttersending = it.localDate("fristEttersending"),
                    linkSoknad = it.stringOrNull("linkSoknad"),
                    journalpostId = it.stringOrNull("journalpostId"),
                    vedlegg = it.json("vedlegg", objectMapper),
                    etterspurteVedlegg = it.json("etterspurteVedlegg", objectMapper),
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
                    mottattTidspunkt,
                    fristEttersending,
                    linkSoknad,
                    journalpostId,
                    vedlegg,
                    etterspurteVedlegg,
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
                    mottattTidspunkt = it.zonedDateTime("mottattTidspunkt"),
                    fristEttersending = it.localDate("fristEttersending"),
                    linkSoknad = it.stringOrNull("linkSoknad"),
                    journalpostId = it.stringOrNull("journalpostId"),
                    vedlegg = it.json("vedlegg", objectMapper),
                    etterspurteVedlegg = it.json("etterspurteVedlegg", objectMapper),
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

    fun oppdaterMottatteVedlegg(soknadsId: String, vedlegg: List<Vedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        vedlegg = :vedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "vedlegg" to vedlegg.toJsonb(objectMapper),
                )
            )
        }
    }

    fun oppdaterEtterspurteVedlegg(soknadsId: String, vedlegg: List<EtterspurtVedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        vedlegg = :vedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "vedlegg" to vedlegg.toJsonb(objectMapper),
                )
            )
        }
    }

    fun oppdaterAlleVedlegg(soknadsId: String, vedlegg: List<Vedlegg>, etterspurteVedlegg: List<EtterspurtVedlegg>) {
        database.update {
            queryOf(
                """
                    update 
                        soknadskvittering
                    set
                        vedlegg = :vedlegg,
                        etterspurteVedlegg = :etterspurteVedlegg
                    where
                        soknadsId = :soknadsId
                """,
                mapOf(
                    "soknadsId" to soknadsId,
                    "vedlegg" to vedlegg.toJsonb(objectMapper),
                    "etterspurteVedlegg" to etterspurteVedlegg.toJsonb(objectMapper)
                )
            )
        }
    }
}
