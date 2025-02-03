package no.nav.tms.soknadskvittering.subscribers

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.observability.withTraceLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.setup.LocalDateHelper.asLocalDate
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
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
    private val objectMapper = defaultObjectMapper()

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {

        val oppdatertEvent: SoknadEvent.SoknadOppdatert = objectMapper.treeToValue(jsonMessage.json)

        repository.updateSoknadsKvittering(
            soknadsId = oppdatertEvent.soknadsId,
            fristEttersending = oppdatertEvent.fristEttersending,
            linkSoknad = oppdatertEvent.linkSoknad,
            journalpostId = oppdatertEvent.journalpostId,
        ).let { wasUpdated ->

            if (wasUpdated) {
                log.info { "Oppdaterte soknadskvittering" }
            } else {
                log.info { "Fant ikke soknadskvittering som skulle oppdateres" }
            }
        }
    }

}
