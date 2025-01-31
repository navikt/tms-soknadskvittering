package no.nav.tms.soknadskvittering.subscribers

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.observability.withTraceLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.setup.LocalDateHelper.asLocalDate
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadOppdatertSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknad_oppdatert")
        .withFields("soknadsId")
        .withOptionalFields(
            "fristEttersending",
            "linkSoknad",
            "journalpostId"
        )

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {

        repository.updateSoknadsKvittering(
            soknadsId = jsonMessage["soknadsId"].asText(),
            fristEttersending = jsonMessage.getOrNull("fristEttersending")?.asLocalDate(),
            linkSoknad = jsonMessage.getOrNull("linkSoknad")?.asText(),
            journalpostId = jsonMessage.getOrNull("journalpostId")?.asText(),
        ).let { wasUpdated ->

            if (wasUpdated) {
                log.info { "Oppdaterte soknadskvittering" }
            } else {
                log.info { "Fant ikke soknadskvittering som skulle oppdateres" }
            }
        }
    }

}
