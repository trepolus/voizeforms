package com.voizeforms

import com.voizeforms.config.MongoConfig
import com.voizeforms.plugins.*
import com.voizeforms.repository.TranscriptionRepositoryFactory
import com.voizeforms.routes.healthRoutes
import com.voizeforms.routes.sessionManagementRoutes
import com.voizeforms.routes.transcriptionRoutes
import com.voizeforms.routes.webRoutes
import com.voizeforms.service.StreamingTranscriptionService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Initialize MongoDB connection
    MongoConfig.init(environment.config)

    // Install plugins
    configureContentNegotiation()
    configureCors()
    configureStatusPages()
    configureMonitoring()
    configureAuthentication()

    // Initialize services with dependency injection
    val transcriptionRepository = TranscriptionRepositoryFactory.createMongoRepository()
    val transcriptionService = StreamingTranscriptionService(transcriptionRepository)
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
        sessionManagementRoutes(transcriptionService)
    }
}
