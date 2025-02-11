package no.nav.tms.soknadskvittering.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tms.soknadskvittering.aggregation.SoknadsKvitteringRepository
import no.nav.tms.soknadskvittering.user
import no.nav.tms.soknadskvittering.userIdent

private const val soknadsIdParameter = "soknadsId"

fun Route.soknadskvitteringRoutes(repository: SoknadsKvitteringRepository) {
    get("/kvitteringer/forenklet/alle") {
        repository.getSoknadskvitteringForUser(userIdent)
            .map(ApiDto::mapSoknadsKvitteringHeader)
            .let {
                call.respond(it)
            }
    }

    get("/kvitteringer/alle") {
        repository.getSoknadskvitteringForUser(userIdent)
            .map(ApiDto::mapSoknadsKvittering)
            .let {
                call.respond(it)
            }
    }

    get("/kvittering/{$soknadsIdParameter}") {
        val kvittering = repository.getSoknadsKvittering(call.soknadsId())

        if (kvittering == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (kvittering.ident != userIdent) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(ApiDto.mapSoknadsKvittering(kvittering))
        }
    }
}


private fun ApplicationCall.soknadsId(): String = parameters[soknadsIdParameter]
    ?: throw IllegalArgumentException("Kallet kan ikke utføres uten at '$soknadsIdParameter' er spesifisert.")
