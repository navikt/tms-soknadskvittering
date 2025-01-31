package no.nav.tms.soknadskvittering.setup

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer("postgres:15")

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate(expectedMigrations = 1)
            }
        }

        fun cleanDb(): LocalPostgresDatabase {
            instance.update { queryOf("delete from soknadskvittering") }
            return instance
        }
    }

    init {
        container.start()
        memDataSource = createDataSource()
    }

    override val dataSource: HikariDataSource
        get() = memDataSource

    private fun createDataSource(): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
            validate()
        }
    }

    private fun migrate(expectedMigrations: Int) {
        Flyway.configure()
            .connectRetries(3)
            .validateMigrationNaming(true)
            .dataSource(dataSource)
            .load()
            .migrate()
            .let { assert(it.migrationsExecuted == expectedMigrations) }

    }
}
