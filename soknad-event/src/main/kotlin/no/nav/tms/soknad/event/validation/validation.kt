package no.nav.tms.soknad.event.validation

import java.net.URI

data class ValidatorResult(
    val isValid: Boolean,
    val explanation: String? = null
)

class SoknadskvitteringValidationException(message: String, val explanation: List<String> = emptyList()): IllegalArgumentException(message)

internal class TextLengthValidator(
    fieldName: String,
    private val maxLength: Int
) {
    val description: String = "$fieldName kan ikke være over $maxLength tegn"

    fun validate(tittel: String?): Boolean {
        return if (tittel != null){
            tittel.length <= maxLength
        } else {
            true
        }
    }
}

internal interface Validator<T> {
    val description: String

    fun assertTrue(validatorFunction: () -> Boolean) = if (validatorFunction()) {
        ValidatorResult(true)
    } else {
        ValidatorResult(false, description)
    }

    fun validate(event: T): ValidatorResult
}

internal fun <T> List<Validator<T>>.validate(event: T) {
    val errors = map {
        it.validate(event)
    }.filterNot { it.isValid }

    if (errors.size > 1) {
        throw SoknadskvitteringValidationException(
            message = "Fant ${errors.size} feil ved validering av soknad-event",
            explanation = errors.mapNotNull { it.explanation }
        )
    } else if (errors.size == 1) {
        throw SoknadskvitteringValidationException(
            message = "Feil ved validering av soknad-event: ${errors.first().explanation}",
            explanation = errors.mapNotNull { it.explanation }
        )
    }
}


internal object LinkContentValidator {
    private const val MAX_LENGTH_LINK = 200
    const val description: String = "Link må være gyldig URL og maks $MAX_LENGTH_LINK tegn"

    fun validate(link: String?): Boolean {
        return link == null || isValidURL(link)
    }

    private fun isValidURL(link: String) =
        link.length <= MAX_LENGTH_LINK && try {
            URI.create(link).toURL()
            true
        } catch (e: IllegalArgumentException) {
            false
        }
}
