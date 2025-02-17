package no.nav.tms.soknadskvittering.builder

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.tms.soknad.event.SoknadEvent
import no.nav.tms.soknad.event.SoknadEvent.Dto.Produsent
import no.nav.tms.soknad.event.validation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object SoknadEventBuilder {
    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun opprettet(builderFunction: SoknadOpprettetInstance.() -> Unit): String {
        return opprettet(SoknadOpprettetInstance(), builderFunction)
    }

    internal fun opprettet(instance: SoknadOpprettetInstance, builderFunction: SoknadOpprettetInstance.() -> Unit): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { SoknadOpprettetValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    fun oppdatert(builderFunction: SoknadOppdatertInstance.() -> Unit): String {
        return oppdatert(SoknadOppdatertInstance(), builderFunction)
    }

    internal fun oppdatert(instance: SoknadOppdatertInstance, builderFunction: SoknadOppdatertInstance.() -> Unit): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { SoknadOppdatertValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    fun ferdigstilt(builderFunction: SoknadFerdigstiltInstance.() -> Unit): String {
        return ferdigstilt(SoknadFerdigstiltInstance(), builderFunction)
    }

    internal fun ferdigstilt(instance: SoknadFerdigstiltInstance, builderFunction: SoknadFerdigstiltInstance.() -> Unit): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .let { objectMapper.writeValueAsString(it) }
    }

    fun vedleggEtterspurtBruker(builderFunction: VedleggEtterspurtBrukerInstance.() -> Unit): String {
        return vedleggEtterspurtBruker(VedleggEtterspurtBrukerInstance(), builderFunction)
    }

    internal fun vedleggEtterspurtBruker(
        instance: VedleggEtterspurtBrukerInstance,
        builderFunction: VedleggEtterspurtBrukerInstance.() -> Unit
    ): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { VedleggEtterspurtValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    fun vedleggEtterspurtTredjepart(builderFunction: VedleggEtterspurtTredjepartInstance.() -> Unit): String {
        return vedleggEtterspurtTredjepart(VedleggEtterspurtTredjepartInstance(), builderFunction)
    }

    internal fun vedleggEtterspurtTredjepart(
        instance: VedleggEtterspurtTredjepartInstance,
        builderFunction: VedleggEtterspurtTredjepartInstance.() -> Unit
    ): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { VedleggEtterspurtValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    fun vedleggMottatt(builderFunction: VedleggMottattInstance.() -> Unit): String {
        return vedleggMottatt(VedleggMottattInstance(), builderFunction)
    }

    internal fun vedleggMottatt(instance: VedleggMottattInstance, builderFunction: VedleggMottattInstance.() -> Unit): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { VedleggMottattValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    fun vedleggOppdatert(builderFunction: VedleggOppdatertInstance.() -> Unit): String {
        return vedleggOppdatert(VedleggOppdatertInstance(), builderFunction)
    }

    internal fun vedleggOppdatert(instance: VedleggOppdatertInstance, builderFunction: VedleggOppdatertInstance.() -> Unit): String {
        instance.builderFunction()
        instance.performNullCheck()

        return instance.build()
            .also { VedleggOppdatertValidation.validate(it) }
            .let { objectMapper.writeValueAsString(it) }
    }

    class SoknadOpprettetInstance internal constructor() {
        var soknadsId: String? = null
        var ident: String? = null
        var tittel: String? = null
        var temakode: String? = null
        var skjemanummer: String? = null
        var tidspunktMottatt: ZonedDateTime? = null
        var fristEttersending: LocalDate? = null
        var linkSoknad: String? = null
        var journalpostId: String? = null
        var produsent: Produsent? = produsent()

        private var mottatteVedlegg: MutableList<MottattVedleggInstance> = mutableListOf()
        private var etterspurteVedlegg: MutableList<EtterspurtVedlegg> = mutableListOf()

        val metadata = metadata()

        fun mottattVedlegg(builder: MottattVedleggInstance.() -> Unit) {
            mottatteVedlegg.add(MottattVedleggInstance().apply(builder))
        }

        fun etterspurtVedlegg(builder: EtterspurtVedlegg.() -> Unit) {
            etterspurteVedlegg.add(EtterspurtVedlegg().apply(builder))
        }

        internal fun build() = SoknadEvent.SoknadOpprettet(
            soknadsId = soknadsId!!,
            ident = ident!!,
            tittel = tittel!!,
            temakode = temakode!!,
            skjemanummer = skjemanummer!!,
            tidspunktMottatt = tidspunktMottatt!!,
            fristEttersending = fristEttersending!!,
            linkSoknad = linkSoknad,
            journalpostId = journalpostId,
            mottatteVedlegg = mottatteVedlegg.map { it.buildDto() },
            etterspurteVedlegg = etterspurteVedlegg.map { it.buildDto() },
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() {
            try {
                requireNotNull(soknadsId) { "soknadsId kan ikke være null" }
                requireNotNull(ident) { "ident kan ikke være null" }
                requireNotNull(tittel) { "tittel kan ikke være null" }
                requireNotNull(temakode) { "temakode kan ikke være null" }
                requireNotNull(skjemanummer) { "skjemanummer kan ikke være null" }
                requireNotNull(tidspunktMottatt) { "tidspunktMottatt kan ikke være null" }
                requireNotNull(fristEttersending) { "fristEttersending kan ikke være null" }
                requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }

                mottatteVedlegg.forEachIndexed { index, vedlegg -> vedlegg.performNullCheck(index) }
                etterspurteVedlegg.forEachIndexed { index, vedlegg -> vedlegg.performNullCheck(index) }
            } catch (e: IllegalArgumentException) {
                throw SoknadskvitteringValidationException(e.message!!)
            }
        }
    }

    class MottattVedleggInstance internal constructor() {
        var vedleggsId: String? = null
        var tittel: String? = null
        var linkVedlegg: String? = null

        fun buildDto() = SoknadEvent.Dto.MottattVedlegg(
            vedleggsId = vedleggsId!!,
            tittel = tittel!!,
            linkVedlegg = linkVedlegg!!
        )

        internal fun performNullCheck(index: Int) {
            try {
                requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
                requireNotNull(tittel) { "tittel kan ikke være null" }
            } catch (e: IllegalArgumentException) {
                throw SoknadskvitteringValidationException("Mottatt vedlegg [$index]: ${e.message!!}")
            }
        }
    }

    class EtterspurtVedlegg internal constructor(
        var vedleggsId: String? = null,
        var brukerErAvsender: Boolean? = null,
        var tittel: String? = null,
        var beskrivelse: String? = null,
        var linkEttersending: String? = null
    ) {
        internal fun buildDto() = SoknadEvent.Dto.EtterspurtVedlegg(
            vedleggsId = vedleggsId!!,
            brukerErAvsender = brukerErAvsender!!,
            tittel = tittel!!,
            beskrivelse = beskrivelse,
            linkEttersending = linkEttersending
        )

        internal fun performNullCheck(index: Int) {
            try {
                requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
                requireNotNull(brukerErAvsender) { "brukerErAvsender kan ikke være null" }
                requireNotNull(tittel) { "tittel kan ikke være null" }
            } catch (e: IllegalArgumentException) {
                throw SoknadskvitteringValidationException("Etterspurt vedlegg [$index]: ${e.message!!}")
            }
        }
    }

    class SoknadOppdatertInstance internal constructor() {
        var soknadsId: String? = null
        var fristEttersending: LocalDate? = null
        var linkSoknad: String? = null
        var journalpostId: String? = null
        var produsent: Produsent? = produsent()

        var metadata: Map<String, Any> = metadata()

        internal fun build() = SoknadEvent.SoknadOppdatert(
            soknadsId = soknadsId!!,
            fristEttersending = fristEttersending,
            linkSoknad = linkSoknad,
            journalpostId = journalpostId,
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "varselId kan ikke være null" }
            requireNotNull(produsent) { "produsent kan ikke være null" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    class SoknadFerdigstiltInstance internal constructor() {
        var soknadsId: String? = null
        var produsent: Produsent? = produsent()

        val metadata = metadata()

        internal fun build() = SoknadEvent.SoknadFerdigstilt(
            soknadsId = soknadsId!!,
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "varselId kan ikke være null" }
            requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    class VedleggEtterspurtBrukerInstance internal constructor() {
        var soknadsId: String? = null
        var vedleggsId: String? = null
        var tittel: String? = null
        var linkEttersending: String? = null
        var tidspunktEtterspurt: ZonedDateTime? = null
        var produsent: Produsent? = produsent()

        val metadata = metadata()

        internal fun build() = SoknadEvent.VedleggEtterspurt(
            brukerErAvsender = true,
            soknadsId = soknadsId!!,
            vedleggsId = vedleggsId!!,
            tittel = tittel!!,
            linkEttersending = linkEttersending!!,
            beskrivelse = null,
            tidspunktEtterspurt = tidspunktEtterspurt!!,
            produsent = produsent!!,
            metadata = metadata,
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "soknadsId kan ikke være null" }
            requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
            requireNotNull(tittel) { "tittel kan ikke være null" }
            requireNotNull(linkEttersending) { "linkEttersending kan ikke være null" }
            requireNotNull(tidspunktEtterspurt) { "tidspunktEtterspurt kan ikke være null" }
            requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    class VedleggEtterspurtTredjepartInstance internal constructor() {
        var soknadsId: String? = null
        var vedleggsId: String? = null
        var tittel: String? = null
        var beskrivelse: String? = null
        var tidspunktEtterspurt: ZonedDateTime? = null
        var produsent: Produsent? = produsent()

        val metadata = metadata()

        internal fun build() = SoknadEvent.VedleggEtterspurt(
            brukerErAvsender = false,
            soknadsId = soknadsId!!,
            vedleggsId = vedleggsId!!,
            tittel = tittel!!,
            linkEttersending = null,
            beskrivelse = beskrivelse,
            tidspunktEtterspurt = tidspunktEtterspurt!!,
            produsent = produsent!!,
            metadata = metadata,
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "soknadsId kan ikke være null" }
            requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
            requireNotNull(tittel) { "tittel kan ikke være null" }
            requireNotNull(tidspunktEtterspurt) { "tidspunktEtterspurt kan ikke være null" }
            requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    class VedleggMottattInstance internal constructor() {
        var soknadsId: String? = null
        var vedleggsId: String? = null
        var tittel: String? = null
        var linkVedlegg: String? = null
        var brukerErAvsender: Boolean? = null
        var tidspunktMottatt: ZonedDateTime? = null
        var produsent: Produsent? = produsent()

        val metadata = metadata()

        internal fun build() = SoknadEvent.VedleggMottatt(
            soknadsId = soknadsId!!,
            vedleggsId = vedleggsId!!,
            tittel = tittel!!,
            linkVedlegg = linkVedlegg,
            brukerErAvsender = brukerErAvsender!!,
            tidspunktMottatt = tidspunktMottatt!!,
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "soknadsId kan ikke være null" }
            requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
            requireNotNull(tittel) { "tittel kan ikke være null" }
            requireNotNull(brukerErAvsender) { "brukerErAvsender kan ikke være null" }
            requireNotNull(tidspunktMottatt) { "tidspunktMottatt kan ikke være null" }
            requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    class VedleggOppdatertInstance internal constructor() {
        var soknadsId: String? = null
        var vedleggsId: String? = null
        var linkVedlegg: String? = null
        var produsent: Produsent? = produsent()

        val metadata = metadata()

        internal fun build() = SoknadEvent.VedleggOppdatert(
            soknadsId = soknadsId!!,
            vedleggsId = vedleggsId!!,
            linkVedlegg = linkVedlegg,
            produsent = produsent!!,
            metadata = metadata
        )

        internal fun performNullCheck() = try {
            requireNotNull(soknadsId) { "soknadsId kan ikke være null" }
            requireNotNull(vedleggsId) { "vedleggsId kan ikke være null" }
            requireNotNull(produsent) { "produsent må spesifiseres manuelt hvis det ikke kan utledes fra env" }
        } catch (e: IllegalArgumentException) {
            throw SoknadskvitteringValidationException(e.message!!)
        }
    }

    private fun produsent(): Produsent? {
        val cluster: String? = BuilderEnvironment.get("NAIS_CLUSTER_NAME")
        val namespace: String? = BuilderEnvironment.get("NAIS_NAMESPACE")
        val appnavn: String? = BuilderEnvironment.get("NAIS_APP_NAME")

        return if (cluster.isNullOrBlank() || namespace.isNullOrBlank() || appnavn.isNullOrBlank()) {
            null
        } else {
            Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = appnavn
            )
        }
    }

    private fun metadata() = mutableMapOf<String, Any>(
        "version" to SoknadEvent.version,
        "built_at" to ZonedDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.MILLIS),
        "builder_lang" to "kotlin"
    )
}

object BuilderEnvironment {
    private val baseEnv = System.getenv()
    private val env = mutableMapOf<String, String>()

    init {
        env.putAll(baseEnv)
    }

    fun extend(extendedEnv: Map<String, String>) {
        env.putAll(extendedEnv)
    }

    fun reset() {
        env.clear()
        env.putAll(baseEnv)
    }

    internal fun get(name: String): String? = env[name]
}
