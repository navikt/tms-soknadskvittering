package no.nav.tms.soknadskvittering

import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.soknadskvittering.setup.Environment
import no.nav.tms.soknadskvittering.setup.PostgresDatabase
import no.nav.tms.soknadskvittering.aggregation.*
import no.nav.tms.soknadskvittering.historikk.HistorikkAppender
import no.nav.tms.soknadskvittering.historikk.HistorikkRepository
import no.nav.tms.soknadskvittering.setup.Flyway

fun main() {
    val environment = Environment()

    val database = PostgresDatabase()

    val repository = SoknadsKvitteringRepository(database)

    val historikkAppender = HistorikkAppender(HistorikkRepository(database))

    KafkaApplication.build {
        kafkaConfig {
            readTopic(environment.kafkaTopic)
            groupId = environment.groupId
        }

        subscribers(
            SoknadInnsendtSubscriber(repository, historikkAppender),
            SoknadOppdatertSubscriber(repository, historikkAppender),
            SoknadFerdigstiltSubscriber(repository, historikkAppender),
            VedleggEtterspurtSubscriber(repository, historikkAppender),
            VedleggMottattSubscriber(repository, historikkAppender),
            VedleggOppdatertSubscriber(repository, historikkAppender)
        )

        ktorModule {
            soknadkvitteringModule(repository)
        }

        onStartup {
            Flyway.runFlywayMigrations()
        }
    }.start()
}
