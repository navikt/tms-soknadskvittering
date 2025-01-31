package no.nav.tms.soknadskvittering

import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.soknadskvittering.setup.Environment
import no.nav.tms.soknadskvittering.setup.PostgresDatabase
import no.nav.tms.soknadskvittering.subscribers.*

fun main() {
    val environment = Environment()

    val repository = SoknadsKvitteringRepository(PostgresDatabase())

    KafkaApplication.build {
        kafkaConfig {
            readTopic(environment.kafkaTopic)
            groupId = environment.groupId
        }

        subscribers(
            SoknadOpprettetSubscriber(repository),
            SoknadOppdatertSubscriber(repository),
            SoknadFerdigstiltSubscriber(repository),
            VedleggEtterspurtSubscriber(repository),
            VedleggMottattSubscriber(repository)
        )
    }
}
