package no.nav.tms.soknadskvittering.setup

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Query
import kotliquery.Row
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction

import kotliquery.sessionOf
import kotliquery.using
import org.postgresql.util.PGobject
import org.postgresql.util.PSQLState
import java.sql.SQLException

interface Database {
    val dataSource: HikariDataSource

    fun update(queryBuilder: () -> Query): Int =
        using(sessionOf(dataSource)) {
            it.run(queryBuilder.invoke().asUpdate)
        }

    fun insert(queryBuilder: () -> Query): Boolean = try {
        using(sessionOf(dataSource)) {
            it.run(queryBuilder.invoke().asUpdate)
        } > 0
    } catch (e: SQLException) {
        if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
            false
        } else {
            throw e
        }
    }

    fun <T> list(action: () -> ListResultQueryAction<T>): List<T> =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

    fun <T> single(action: () -> NullableResultQueryAction<T>): T =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        } ?: throw IllegalStateException("Must return some value")

    fun <T> singleOrNull(action: () -> NullableResultQueryAction<T>): T? =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }
}

inline fun <reified T> Row.json(label: String, objectMapper: ObjectMapper = defaultObjectMapper()): T {
    return objectMapper.readValue(string(label))
}

inline fun <reified T> Row.jsonOrNull(label: String, objectMapper: ObjectMapper = defaultObjectMapper()): T? {
    return stringOrNull(label)?.let { objectMapper.readValue(it) }
}

fun Any?.toJsonb(objectMapper: ObjectMapper = defaultObjectMapper()): PGobject? {
    return if (this == null) {
        null
    } else {
        objectMapper.writeValueAsString(this).let {
            PGobject().apply {
                type = "jsonb"
                value = it
            }
        }
    }
}
