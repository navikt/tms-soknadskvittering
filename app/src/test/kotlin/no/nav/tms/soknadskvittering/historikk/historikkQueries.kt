package no.nav.tms.soknadskvittering.historikk

import kotliquery.queryOf
import no.nav.tms.soknadskvittering.setup.Database
import no.nav.tms.soknadskvittering.setup.json
import no.nav.tms.soknadskvittering.setup.jsonOrNull

fun Database.firstHistorikkEntry(soknadsId: String, eventName: String? = null): HistorikkEntry? {
    return singleOrNull {
        queryOf(
            """
                select 
                    soknadsId, event, innhold, produsent, tidspunkt from soknadsevent_historikk 
                where
                    soknadsId = :soknadsId ${if (eventName != null) " and event = :event " else ""}
            """,
            mapOf("soknadsId" to soknadsId, "event" to eventName)
        ).map {
            HistorikkEntry(
                soknadsId = it.string("soknadsId"),
                event = it.string("event"),
                innhold = it.jsonOrNull("innhold"),
                produsent = it.json("produsent"),
                tidspunkt = it.zonedDateTime("tidspunkt")
            )
        }.asSingle
    }
}

fun Database.getHistorikkEntries(soknadsId: String, eventName: String? = null): List<HistorikkEntry> {
    return list {
        queryOf(
            """
                select 
                    soknadsId, event, innhold, produsent, tidspunkt from soknadsevent_historikk 
                where
                    soknadsId = :soknadsId ${if (eventName != null) " and event = :event " else ""}
            """,
            mapOf("soknadsId" to soknadsId, "event" to eventName)
        ).map {
            HistorikkEntry(
                soknadsId = it.string("soknadsId"),
                event = it.string("event"),
                innhold = it.jsonOrNull("innhold"),
                produsent = it.json("produsent"),
                tidspunkt = it.zonedDateTime("tidspunkt")
            )
        }.asList
    }
}
