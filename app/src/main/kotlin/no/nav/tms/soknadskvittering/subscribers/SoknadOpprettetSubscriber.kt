package no.nav.tms.soknadskvittering.subscribers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.*
import no.nav.tms.soknadskvittering.EtterspurtVedlegg
import no.nav.tms.soknadskvittering.SoknadsKvittering
import no.nav.tms.soknadskvittering.MottattVedlegg
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
            "tidspunktMottatt",
            "fristEttersending",
            "mottatteVedlegg",
            "etterspurteVedlegg"
        )
        .withOptionalFields(
            "linkSoknad",
            "journalpostId"
        )

    private val log = KotlinLogging.logger {}

    override suspend fun receive(jsonMessage: JsonMessage) = withMDC(jsonMessage) {
        val vedlegg = jsonMessage.list("mottatteVedlegg").map { vedleggNode ->
            MottattVedlegg(
                vedleggsId = vedleggNode["vedleggsId"].asText(),
                tittel = vedleggNode["tittel"].asText(),
                linkVedlegg = vedleggNode["linkVedlegg"]?.asText(),
                brukerErAvsender = true,
                tidspunktMottatt = jsonMessage["tidspunktMottatt"].asZonedDateTime()
            )
        }

        val etterspurteVedlegg = jsonMessage.list("etterspurteVedlegg").map { etterspurtNode ->
            EtterspurtVedlegg(
                vedleggsId = etterspurtNode["vedleggsId"].asText(),
                brukerErAvsender = etterspurtNode["brukerErAvsender"].asBoolean(),
                tittel = etterspurtNode["tittel"].asText(),
                linkEttersending = etterspurtNode["linkEttersending"]?.asText(),
                beskrivelse = etterspurtNode["beskrivelse"]?.asText(),
                tidspunktEtterspurt = jsonMessage["tidspunktMottatt"].asZonedDateTime(),
            )
        }

        val soknadsKvittering = SoknadsKvittering(
            soknadsId = jsonMessage["soknadsId"].asText(),
            ident = jsonMessage["ident"].asText(),
            tittel = jsonMessage["tittel"].asText(),
            temakode = jsonMessage["temakode"].asText(),
            skjemanummer = jsonMessage["skjemanummer"].asText(),
            tidspunktMottatt = jsonMessage["tidspunktMottatt"].asZonedDateTime(),
            fristEttersending = jsonMessage["fristEttersending"].asLocalDate(),
            linkSoknad = jsonMessage.getOrNull("linkSoknad")?.asText(),
            journalpostId = jsonMessage.getOrNull("journalpostId")?.asText(),
            mottatteVedlegg = vedlegg,
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

    private val objectMapper = jacksonObjectMapper()

    private fun JsonMessage.list(fieldName: String): List<JsonNode> {
        val field = get(fieldName)

        if (!field.isArray) {
            throw MessageException("Field $fieldName was not an array node")
        }

        return field.map {
            val objectNode = objectMapper.createObjectNode()

            it.fields().forEach { (name, value) ->
                it.get(name)
                    .takeUnless { it.isMissingOrNull() }
                    ?.let { objectNode.replace(name, value) }
            }

            objectNode
        }
    }
}
