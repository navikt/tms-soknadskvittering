package no.nav.tms.soknadskvittering.historikk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknadskvittering.setup.ZonedDateTimeHelper
import no.nav.tms.soknadskvittering.setup.defaultObjectMapper
import java.time.ZonedDateTime

class HistorikkAppender(private val repository: HistorikkRepository) {

    private val objectMapper = initializeFilteredMapper("eventName", "soknadsId", "produsent", "metadata")

    fun soknadInnsendt(event: SoknadEvent.SoknadInnsendt) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = objectMapper.valueToTree(event),
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }

    fun soknadOppdatert(event: SoknadEvent.SoknadOppdatert) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = objectMapper.valueToTree(event),
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }

    fun soknadFerdigstilt(event: SoknadEvent.SoknadFerdigstilt) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = null,
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }

    fun vedleggEtterspurt(event: SoknadEvent.VedleggEtterspurt) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = objectMapper.valueToTree(event),
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }

    fun vedleggMottatt(event: SoknadEvent.VedleggMottatt) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = objectMapper.valueToTree(event),
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }


    fun vedleggOppdatert(event: SoknadEvent.VedleggOppdatert) {
        HistorikkEntry(
            soknadsId = event.soknadsId,
            event = event.eventName,
            innhold = objectMapper.valueToTree(event),
            produsent = event.produsent,
            tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        ).let {
            repository.appendEvent(it)
        }
    }

    fun initializeFilteredMapper(vararg ignoredFields: String) = defaultObjectMapper().setAnnotationIntrospector(
        object : JacksonAnnotationIntrospector() {
            override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
                return ignoredFields.contains(m.name) || super.hasIgnoreMarker(m)
            }
        }
    )
}

data class HistorikkEntry(
    val soknadsId: String,
    val event: String,
    val innhold: JsonNode?,
    val produsent: SoknadEvent.Dto.Produsent,
    val tidspunkt: ZonedDateTime
)
