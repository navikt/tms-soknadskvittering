package no.nav.tms.soknadskvittering.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tms.soknadskvittering.aggregation.SoknadsKvitteringRepository
import no.nav.tms.soknadskvittering.user

private const val soknadsIdParameter = "soknadsId"

fun Route.soknadskvitteringRoutes(repository: SoknadsKvitteringRepository) {
    get("/kvitteringer/forenklet/alle") {
        repository.getSoknadskvitteringForUser(user.ident)
            .map(ApiDto::mapSoknadsKvitteringHeader)
            .let {
                call.respond(it)
            }
    }

    get("/kvitteringer/alle") {
        repository.getSoknadskvitteringForUser(user.ident)
            .map(ApiDto::mapSoknadsKvittering)
            .let {
                call.respond(it)
            }
    }

    get("/kvittering/{$soknadsIdParameter}") {
        val kvittering = repository.getSoknadsKvittering(soknadsIdParameter)

        if (kvittering == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (kvittering.ident != user.ident) {
            call.respond(HttpStatusCode.Forbidden)
        } else {
            call.respond(ApiDto.mapSoknadsKvittering(kvittering))
        }
    }
}


private fun ApplicationCall.soknadsId(): String = parameters[soknadsIdParameter]
    ?: throw IllegalArgumentException("Kallet kan ikke utføres uten at '$soknadsIdParameter' er spesifisert.")
