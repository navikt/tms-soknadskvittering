package no.nav.tms.soknad.event.validation

import no.nav.tms.soknad.event.SoknadEvent.VedleggOppdatert


object VedleggOppdatertValidation {

    private val validators: List<VedleggOppdatertValidator> = listOf(
        LinkVedleggValidator,
    )

    fun validate(vedleggOppdatert: VedleggOppdatert) = validators.validate(vedleggOppdatert)

    private interface VedleggOppdatertValidator: Validator<VedleggOppdatert>

    private object LinkVedleggValidator: VedleggOppdatertValidator {
        override val description = "Feil i link til mottatt vedlegg. ${LinkContentValidator.description}"

        override fun validate(event: VedleggOppdatert) = assertTrue {
            LinkContentValidator.validate(event.linkVedlegg)
        }
    }
}
