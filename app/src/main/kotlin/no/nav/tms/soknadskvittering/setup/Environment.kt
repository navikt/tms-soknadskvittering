package no.nav.tms.soknadskvittering.setup

import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val kafkaTopic: String = getEnvVar("KAFKA_TOPIC"),
    val groupId: String = getEnvVar("KAFKA_GROUP_ID")
)
