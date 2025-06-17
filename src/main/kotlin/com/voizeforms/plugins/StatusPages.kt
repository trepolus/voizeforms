package com.voizeforms.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.voizeforms.model.ErrorInfo
import com.voizeforms.model.ErrorResponse
import kotlinx.serialization.json.Json

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            val errorResponse = ErrorResponse(
                error = ErrorInfo(
                    code = "INTERNAL_SERVER_ERROR",
                    message = cause.message ?: "Internal server error",
                    details = emptyMap()
                )
            )
            call.respond(HttpStatusCode.InternalServerError, errorResponse)
        }
        status(HttpStatusCode.BadRequest) { call, _ ->
            val errorResponse = ErrorResponse(
                error = ErrorInfo(
                    code = "BAD_REQUEST",
                    message = "Invalid request",
                    details = emptyMap()
                )
            )
            call.respond(HttpStatusCode.BadRequest, errorResponse)
        }
    }
} 