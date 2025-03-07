package no.nav.tms.soknadskvittering.historikk

import kotliquery.queryOf
import no.nav.tms.soknadskvittering.setup.Database
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import no.nav.tms.soknadskvittering.setup.toJsonb

class HistorikkRepository(private val database: Database) {

    private val objectMapper = defaultObjectMapper()

    fun appendEvent(entry: HistorikkEntry) {
        database.update {
            queryOf("""
                insert into soknadsevent_historikk(
                    soknadsId,
                    event,
                    innhold,
                    produsent,
                    tidspunkt
                ) values (
                    :soknadsId,
                    :event,
                    :innhold,
                    :produsent,
                    :tidspunkt
                )
            """, mapOf(
                "soknadsId" to entry.soknadsId,
                "event" to entry.event,
                "innhold" to entry.innhold.toJsonb(objectMapper),
                "produsent" to entry.produsent.toJsonb(objectMapper),
                "tidspunkt" to entry.tidspunkt
            ))
        }
    }
}
