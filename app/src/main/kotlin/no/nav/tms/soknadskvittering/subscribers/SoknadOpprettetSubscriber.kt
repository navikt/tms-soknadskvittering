package no.nav.tms.soknadskvittering.subscribers

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.soknadskvittering.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.SoknadsKvittering
import no.nav.tms.soknadskvittering.Vedlegg
import no.nav.tms.soknadskvittering.setup.LocalDateHelper.asLocalDate
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper.asZonedDateTime
import no.nav.tms.soknadskvittering.setup.withMDC

class SoknadOpprettetSubscriber(private val repository: SoknadsKvitteringRepository): Subscriber() {

    override fun subscribe() = Subscription.forEvent("soknad_opprettet")
        .withFields(
            "soknadsId",
            "ident",
            "tittel",
            "temakode",
            "skjemanummer",
            "mottattTidspunkt",
            "fristEttersending",
            "vedlegg",
            "etterspurteVedlegg"
        )
        .withOptionalFields(
            "linkSoknad",
            "journalpostId"
        )

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val vedlegg = jsonMessage["vedlegg"].map { vedleggNode ->
            Vedlegg(
                vedleggsId = vedleggNode["vedleggsId"]!!.asText(),
                tittel = vedleggNode["tittel"]!!.asText(),
                linkVedlegg = vedleggNode["linkVedlegg"].asText(),
                brukerErAvsender = true,
                tidspunktMottatt = jsonMessage["mottattTidspunkt"].asZonedDateTime()
            )
        }

        val etterspurteVedlegg = jsonMessage["etterspurteVedlegg"].map { etterspurtNode ->
            EtterspurtVedlegg(
                vedleggsId = etterspurtNode["vedleggsId"]!!.asText(),
                brukerErAvsender = etterspurtNode["brukerErAvsender"]!!.asBoolean(),
                tittel = etterspurtNode["tittel"]!!.asText(),
                linkEttersending = etterspurtNode["linkEttersending"].asText(),
                beskrivelse = etterspurtNode["beskrivelse"].asText(),
                tidspunktEtterspurt = etterspurtNode["tidspunktEtterspurt"].asZonedDateTime(),
            )
        }

        val soknadsKvittering = SoknadsKvittering(
            soknadsId = jsonMessage["soknadsId"].asText(),
            ident = jsonMessage["ident"].asText(),
            tittel = jsonMessage["tittel"].asText(),
            temakode = jsonMessage["temakode"].asText(),
            skjemanummer = jsonMessage["skjemanummer"].asText(),
            mottattTidspunkt = jsonMessage["mottattTidspunkt"].asZonedDateTime(),
            fristEttersending = jsonMessage["fristEttersending"].asLocalDate(),
            linkSoknad = jsonMessage.getOrNull("linkSoknad")?.asText(),
            journalpostId = jsonMessage.getOrNull("journalpostId")?.asText(),
            vedlegg = vedlegg,
            etterspurteVedlegg = etterspurteVedlegg,
            opprettet = ZonedDateTimeHelper.nowAtUtc(),
            ferdigstilt = null
        )

        repository.insertSoknadsKvittering(soknadsKvittering).let { wasCreated ->
            if (wasCreated) {
                log.info { "Opprettet ny soknadskvittering" }
            } else {
                log.info { "Ignorerte duplikat soknadskvittering" }
            }
        }
    }

}
