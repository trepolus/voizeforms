package com.voizeforms

import com.voizeforms.config.configureOAuth
import com.voizeforms.routes.healthRoutes
import com.voizeforms.routes.transcriptionRoutes
import com.voizeforms.routes.webRoutes
import com.voizeforms.service.MockTranscriptionService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Configure JSON serialization
    install(ContentNegotiation) {
        json()
    }

    // Configure OAuth and sessions
    configureOAuth()

    // Initialize services
    val transcriptionService = MockTranscriptionService()
    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    // Configure routing
    routing {
        healthRoutes()
        webRoutes(httpClient)
        transcriptionRoutes(transcriptionService)
    }
}
