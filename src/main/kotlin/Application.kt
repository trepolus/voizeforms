package com.voizeforms

import com.voizeforms.routes.healthRoutes
import com.voizeforms.routes.transcriptionRoutes
import com.voizeforms.service.MockTranscriptionService
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

    // Initialize services
    val transcriptionService = MockTranscriptionService()

    // Configure routing
    routing {
        healthRoutes()
        transcriptionRoutes(transcriptionService)
    }
}
